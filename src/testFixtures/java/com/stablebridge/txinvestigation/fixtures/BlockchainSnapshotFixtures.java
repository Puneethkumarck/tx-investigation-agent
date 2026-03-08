package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.BlockchainSnapshot;

import java.math.BigDecimal;
import java.time.Instant;

public final class BlockchainSnapshotFixtures {

    private BlockchainSnapshotFixtures() {}

    public static BlockchainSnapshot aBlockchainSnapshot() {
        return new BlockchainSnapshot(
                InvestigationQueryFixtures.PAYMENT_ID,
                "0xabc123def456",
                "base",
                2,
                new BigDecimal("1000.00"),
                "USDC",
                "0xSender123",
                "0xReceiver456",
                Instant.parse("2026-03-08T10:31:00Z"),
                "PENDING");
    }
}
