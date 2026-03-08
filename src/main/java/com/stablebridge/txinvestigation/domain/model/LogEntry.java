package com.stablebridge.txinvestigation.domain.model;

import java.time.Instant;

public record LogEntry(
        Instant timestamp,
        LogLevel level,
        String service,
        String message,
        String traceId,
        String stackTrace
) {}
