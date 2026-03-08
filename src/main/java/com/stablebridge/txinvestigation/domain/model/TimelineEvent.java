package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;

public record TimelineEvent(
        Instant timestamp,
        String service,
        String description,
        String status
) {}
