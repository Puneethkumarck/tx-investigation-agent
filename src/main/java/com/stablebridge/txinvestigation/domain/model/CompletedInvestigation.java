package com.stablebridge.txinvestigation.domain.model;

public record CompletedInvestigation(
        InvestigationQuery query,
        PaymentState paymentState,
        ComplianceSnapshot complianceSnapshot,
        BlockchainSnapshot blockchainSnapshot,
        LedgerSnapshot ledgerSnapshot,
        InvestigationReport report,
        String formattedBody
) {}
