package com.stablebridge.txinvestigation.application.controller;

import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
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
    private final ReportFormatter reportFormatter;

    @PostMapping
    public ResponseEntity<InvestigationResponse> investigate(
            @Valid @RequestBody InvestigationRequest request) {

        var paymentState = paymentStateProvider.fetchPaymentState(request.paymentId());
        var complianceSnapshot = complianceStateProvider.fetchComplianceStatus(request.paymentId());
        var blockchainSnapshot = blockchainStateProvider.fetchBlockchainStatus(request.paymentId());
        var ledgerSnapshot = ledgerStateProvider.fetchLedgerEntries(request.paymentId());

        // For the REST endpoint, we provide a summary without LLM analysis
        var response = InvestigationResponse.builder()
                .paymentId(request.paymentId())
                .status(paymentState.status())
                .severity(null)
                .rootCause("Data collected — use GOAP agent for full LLM analysis")
                .findings(List.of())
                .timeline(List.of())
                .recommendations(List.of())
                .formattedReport(reportFormatter.formatTimeline(
                        paymentState.events().stream()
                                .map(e -> new com.stablebridge.txinvestigation.domain.model.TimelineEvent(
                                        e.timestamp(), "S1 Orchestrator", e.detail(), e.status()))
                                .toList()))
                .build();

        return ResponseEntity.ok(response);
    }
}
