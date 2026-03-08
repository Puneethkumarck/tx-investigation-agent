package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;
import java.util.List;

public record PaymentState(
        String paymentId,
        PaymentStatus status,
        String sagaStep,
        String workflowId,
        List<SagaEvent> events,
        Instant createdAt,
        Instant updatedAt
) {}
