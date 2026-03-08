package com.stablebridge.txinvestigation.application.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

        var response = InvestigationResponse.builder()
                .paymentId(request.paymentId())
                .status(paymentState.status())
                .severity(null)
                .rootCause("Data collected — use GOAP agent for full LLM analysis")
                .findings(List.of())
                .timeline(List.of())
                .recommendations(List.of())
                .errorLogCount(logSnapshot.totalHits())
                .traceId(traceSnapshot.traceId())
                .workflowStatus(workflowSnapshot.status())
                .formattedReport(reportFormatter.formatTimeline(
                        paymentState.events().stream()
                                .map(e -> new com.stablebridge.txinvestigation.domain.model.TimelineEvent(
                                        e.timestamp(), "S1 Orchestrator", e.detail(), e.status()))
                                .toList()))
                .build();

        return ResponseEntity.ok(response);
    }
}
