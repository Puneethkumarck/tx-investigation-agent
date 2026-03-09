package com.stablebridge.txinvestigation.application.controller;

import com.stablebridge.txinvestigation.domain.model.InvestigationReport;
import com.stablebridge.txinvestigation.domain.model.TimelineEvent;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.LogSearchProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import com.stablebridge.txinvestigation.domain.port.TraceProvider;
import com.stablebridge.txinvestigation.domain.port.WorkflowHistoryProvider;
import com.stablebridge.txinvestigation.domain.service.ReportFormatter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/investigations")
@RequiredArgsConstructor
public class InvestigationController {

    private final PaymentStateProvider paymentStateProvider;
    private final ComplianceStateProvider complianceStateProvider;
    private final BlockchainStateProvider blockchainStateProvider;
    private final LedgerStateProvider ledgerStateProvider;
    private final WorkflowHistoryProvider workflowHistoryProvider;
    private final LogSearchProvider logSearchProvider;
    private final TraceProvider traceProvider;
    private final ReportFormatter reportFormatter;
    private final ChatClient.Builder chatClientBuilder;

    @PostMapping
    public ResponseEntity<InvestigationResponse> investigate(
            @Valid @RequestBody InvestigationRequest request) {

        var paymentState = paymentStateProvider.fetchPaymentState(request.paymentId());
        var complianceSnapshot = complianceStateProvider.fetchComplianceStatus(request.paymentId());
        var blockchainSnapshot = blockchainStateProvider.fetchBlockchainStatus(request.paymentId());
        var ledgerSnapshot = ledgerStateProvider.fetchLedgerEntries(request.paymentId());
        var workflowSnapshot = workflowHistoryProvider.fetchWorkflowHistory(request.paymentId());
        var logSnapshot = logSearchProvider.searchErrorLogs(request.paymentId());
        var traceSnapshot = traceProvider.fetchTrace(request.paymentId());

        var prompt = """
                You are a Senior Blockchain Payments Engineer with deep expertise in
                cross-border stablecoin payment infrastructure. Analyze the following payment
                data from multiple services and produce a JSON investigation report.

                ## Payment State (Orchestrator)
                Payment ID: %s
                Status: %s
                Saga Step: %s
                Workflow ID: %s
                Events: %s

                ## Compliance Status
                Screening: %s
                Travel Rule: %s
                Risk Score: %.2f
                Decisions: %s

                ## Blockchain Status
                TX Hash: %s
                Chain: %s
                Confirmations: %d
                Amount: %s %s
                Status: %s

                ## Ledger Status
                Entries: %s
                Net Position: %s
                Settlement: %s

                ## Workflow History (Temporal)
                Workflow ID: %s
                Type: %s
                Status: %s
                Start: %s
                Attempts: %d
                Events: %s

                ## Error Logs (Elasticsearch)
                Total Hits: %d
                Entries: %s

                ## Distributed Trace
                Trace ID: %s
                Total Spans: %d
                Duration: %d ms
                Spans: %s

                ## Instructions
                1. Build a chronological timeline of events across all services
                2. Identify the SINGLE root cause with specific evidence:
                   - Which service failed, which step, what error
                   - Example: "Custody service failed to submit tx 0xabc123 — gas price spike to 45 gwei exceeded 20 gwei limit at saga step BLOCKCHAIN_SUBMIT"
                   - NOT vague like "blockchain error" or "transaction stuck"
                3. List findings with severity (CRITICAL, HIGH, MEDIUM, LOW, INFO)
                   and category (STUCK_PAYMENT, COMPLIANCE_BLOCK, BLOCKCHAIN_DELAY,
                   SETTLEMENT_MISMATCH, SLA_BREACH, RECONCILIATION_GAP,
                   WORKFLOW_FAILURE, ERROR_SPIKE, LATENCY_ANOMALY)
                4. Provide exactly ONE recommendation that is:
                   - A specific action an engineer can take RIGHT NOW
                   - References the exact service, transaction ID, or endpoint involved
                   - Example: "Resubmit transaction 0xabc123 via custody service /api/v1/retry with updated gas fee"
                   - NOT generic advice like "monitor the situation" or "investigate further"
                5. Set overall severity based on the most critical finding
                6. Correlate workflow events with log errors to identify failed activities
                7. Check trace spans for latency anomalies (>30s for any single span)
                """.formatted(
                paymentState.paymentId(),
                paymentState.status(),
                paymentState.sagaStep(),
                paymentState.workflowId(),
                paymentState.events(),
                complianceSnapshot.screeningResult(),
                complianceSnapshot.travelRuleStatus(),
                complianceSnapshot.riskScore(),
                complianceSnapshot.decisions(),
                blockchainSnapshot.txHash(),
                blockchainSnapshot.chain(),
                blockchainSnapshot.confirmations(),
                blockchainSnapshot.amount(),
                blockchainSnapshot.currency(),
                blockchainSnapshot.status(),
                ledgerSnapshot.entries(),
                ledgerSnapshot.netPosition(),
                ledgerSnapshot.settlementStatus(),
                workflowSnapshot.workflowId(),
                workflowSnapshot.workflowType(),
                workflowSnapshot.status(),
                workflowSnapshot.startTime(),
                workflowSnapshot.attemptCount(),
                workflowSnapshot.events(),
                logSnapshot.totalHits(),
                logSnapshot.entries(),
                traceSnapshot.traceId(),
                traceSnapshot.totalSpans(),
                traceSnapshot.durationMs(),
                traceSnapshot.spans());

        log.info("Sending investigation data to LLM for analysis of payment {}", request.paymentId());

        var report = chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .call()
                .entity(InvestigationReport.class);

        log.info("LLM analysis complete for payment {} — severity: {}", request.paymentId(),
                report.severity());

        var formattedReport = reportFormatter.format(report, paymentState);

        var response = InvestigationResponse.builder()
                .paymentId(request.paymentId())
                .status(paymentState.status())
                .severity(report.severity())
                .rootCause(report.rootCause())
                .findings(report.findings())
                .timeline(report.timeline())
                .recommendation(report.recommendation())
                .errorLogCount(logSnapshot.totalHits())
                .traceId(traceSnapshot.traceId())
                .workflowStatus(workflowSnapshot.status())
                .formattedReport(formattedReport)
                .build();

        return ResponseEntity.ok(response);
    }
}
