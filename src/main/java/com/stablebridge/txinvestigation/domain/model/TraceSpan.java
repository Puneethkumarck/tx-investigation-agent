package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;

public record TraceSpan(
        String spanId,
        String parentSpanId,
        String operationName,
        String serviceName,
        Instant startTime,
        long durationMs,
        String status,
        String errorMessage
) {}
