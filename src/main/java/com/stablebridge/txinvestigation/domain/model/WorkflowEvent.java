package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;

public record WorkflowEvent(
        long eventId,
        String eventType,
        Instant timestamp,
        String activityType,
        String detail
) {}
