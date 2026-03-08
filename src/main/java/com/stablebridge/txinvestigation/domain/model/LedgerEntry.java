package com.stablebridge.txinvestigation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntry(
        String entryId,
        String account,
        Direction direction,
        BigDecimal amount,
        String currency,
        Instant timestamp
) {}
