package com.stablebridge.txinvestigation.domain.model;

public record CompletedInvestigation(
        InvestigationQuery query,
        PaymentState paymentState,
        ComplianceSnapshot complianceSnapshot,
        BlockchainSnapshot blockchainSnapshot,
        LedgerSnapshot ledgerSnapshot,
        WorkflowSnapshot workflowSnapshot,
        LogSnapshot logSnapshot,
        TraceSnapshot traceSnapshot,
        InvestigationReport report,
        String formattedBody
) {}
