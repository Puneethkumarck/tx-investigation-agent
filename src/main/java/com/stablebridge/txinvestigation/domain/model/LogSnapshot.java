package com.stablebridge.txinvestigation.domain.model;

import java.util.List;

public record LogSnapshot(
        String paymentId,
        int totalHits,
        List<LogEntry> entries
) {}
