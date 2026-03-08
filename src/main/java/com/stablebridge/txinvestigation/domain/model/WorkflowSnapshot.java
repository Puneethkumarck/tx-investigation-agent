package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;
import java.util.List;

public record WorkflowSnapshot(
        String paymentId,
        String workflowId,
        String workflowType,
        String status,
        Instant startTime,
        Instant closeTime,
        int attemptCount,
        String taskQueue,
        List<WorkflowEvent> events
) {}
