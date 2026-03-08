package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.LedgerEntry;
import com.stablebridge.txinvestigation.domain.model.LedgerSnapshot;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.domain.model.Direction.CREDIT;
import static com.stablebridge.txinvestigation.domain.model.Direction.DEBIT;

public final class LedgerSnapshotFixtures {

    private LedgerSnapshotFixtures() {}

    public static LedgerEntry aDebitEntry() {
        return new LedgerEntry(
                "LE-001",
                "MERCHANT_USD",
                DEBIT,
                new BigDecimal("1000.00"),
                "USD",
                Instant.parse("2026-03-08T10:15:00Z"));
    }

    public static LedgerEntry aCreditEntry() {
        return new LedgerEntry(
                "LE-002",
                "POOL_USDC",
                CREDIT,
                new BigDecimal("1000.00"),
                "USDC",
                Instant.parse("2026-03-08T10:31:00Z"));
    }

    public static LedgerSnapshot aLedgerSnapshot() {
        return new LedgerSnapshot(
                InvestigationQueryFixtures.PAYMENT_ID,
                List.of(aDebitEntry(), aCreditEntry()),
                BigDecimal.ZERO,
                "PENDING");
    }
}
