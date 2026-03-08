package com.stablebridge.txinvestigation.domain.model;

import java.util.List;

public record TraceSnapshot(
        String paymentId,
        String traceId,
        int totalSpans,
        long durationMs,
        List<TraceSpan> spans
) {}
