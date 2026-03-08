package com.stablebridge.txinvestigation.domain.model;

import java.util.List;

public record ComplianceSnapshot(
        String paymentId,
        ScreeningResult screeningResult,
        String travelRuleStatus,
        double riskScore,
        List<ComplianceDecision> decisions
) {}
