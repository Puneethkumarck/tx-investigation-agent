package com.stablebridge.txinvestigation.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record LedgerSnapshot(
        String paymentId,
        List<LedgerEntry> entries,
        BigDecimal netPosition,
        String settlementStatus
) {}
