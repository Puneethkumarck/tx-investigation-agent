package com.stablebridge.txinvestigation.domain.model;

public record Finding(
        FindingCategory category,
        InvestigationSeverity severity,
        String description
) {}
