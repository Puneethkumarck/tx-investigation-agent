package com.stablebridge.txinvestigation.shell;

import com.stablebridge.txinvestigation.domain.model.TimelineEvent;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.LogSearchProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import com.stablebridge.txinvestigation.domain.port.TraceProvider;
import com.stablebridge.txinvestigation.domain.port.WorkflowHistoryProvider;
import com.stablebridge.txinvestigation.domain.service.ReportFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@Slf4j
@ShellComponent
@RequiredArgsConstructor
public class InvestigationCommands {

    private final PaymentStateProvider paymentStateProvider;
    private final ComplianceStateProvider complianceStateProvider;
    private final BlockchainStateProvider blockchainStateProvider;
    private final LedgerStateProvider ledgerStateProvider;
    private final WorkflowHistoryProvider workflowHistoryProvider;
    private final LogSearchProvider logSearchProvider;
    private final TraceProvider traceProvider;
    private final ReportFormatter reportFormatter;

    @ShellMethod(key = "investigate", value = "Investigate a payment's lifecycle across all services")
    public String investigate(
            @ShellOption(help = "Payment ID to investigate") String paymentId,
            @ShellOption(help = "Merchant ID (optional)", defaultValue = "") String merchantId) {

        log.info("Investigating payment {}", paymentId);

        var paymentState = paymentStateProvider.fetchPaymentState(paymentId);
        var complianceSnapshot = complianceStateProvider.fetchComplianceStatus(paymentId);
        var blockchainSnapshot = blockchainStateProvider.fetchBlockchainStatus(paymentId);
        var ledgerSnapshot = ledgerStateProvider.fetchLedgerEntries(paymentId);
        var workflowSnapshot = workflowHistoryProvider.fetchWorkflowHistory(paymentId);
        var logSnapshot = logSearchProvider.searchErrorLogs(paymentId);
        var traceSnapshot = traceProvider.fetchTrace(paymentId);

        var sb = new StringBuilder();
        sb.append("\n--- Investigation: %s ---\n\n".formatted(paymentId));
        sb.append("Status: %s\n".formatted(paymentState.status()));
        sb.append("Saga Step: %s\n".formatted(paymentState.sagaStep()));
        sb.append("Compliance: %s (risk: %.2f)\n".formatted(
                complianceSnapshot.screeningResult(), complianceSnapshot.riskScore()));
        sb.append("Blockchain: %s (%d confirmations)\n".formatted(
                blockchainSnapshot.status(), blockchainSnapshot.confirmations()));
        sb.append("Ledger: %s (net: %s)\n".formatted(
                ledgerSnapshot.settlementStatus(), ledgerSnapshot.netPosition()));
        sb.append("Workflow: %s (%d events, status: %s)\n".formatted(
                workflowSnapshot.workflowId(), workflowSnapshot.events().size(),
                workflowSnapshot.status()));
        sb.append("Logs: %d error/warn entries found\n".formatted(logSnapshot.totalHits()));
        sb.append("Trace: %s (%d spans, %d ms)\n\n".formatted(
                traceSnapshot.traceId(), traceSnapshot.totalSpans(),
                traceSnapshot.durationMs()));

        var timelineEvents = paymentState.events().stream()
                .map(e -> new TimelineEvent(e.timestamp(), "S1 Orchestrator", e.detail(), e.status()))
                .toList();
        sb.append(reportFormatter.formatTimeline(timelineEvents));

        return sb.toString();
    }
}
