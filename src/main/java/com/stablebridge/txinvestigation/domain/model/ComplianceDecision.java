package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;

public record ComplianceDecision(
        String checkType,
        String result,
        String provider,
        Instant timestamp,
        String detail
) {}
