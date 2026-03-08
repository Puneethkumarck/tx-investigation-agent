package com.stablebridge.txinvestigation.domain.model;

import java.util.List;

public record InvestigationReport(
        List<TimelineEvent> timeline,
        String rootCause,
        List<Finding> findings,
        List<String> recommendations,
        InvestigationSeverity severity
) {}
