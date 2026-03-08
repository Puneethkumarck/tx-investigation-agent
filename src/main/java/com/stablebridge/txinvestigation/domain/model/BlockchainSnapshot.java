package com.stablebridge.txinvestigation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record BlockchainSnapshot(
        String paymentId,
        String txHash,
        String chain,
        int confirmations,
        BigDecimal amount,
        String currency,
        String fromAddress,
        String toAddress,
        Instant blockTimestamp,
        String status
) {}
