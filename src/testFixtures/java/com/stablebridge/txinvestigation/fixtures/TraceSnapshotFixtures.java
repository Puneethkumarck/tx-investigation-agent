package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.TraceSnapshot;
import com.stablebridge.txinvestigation.domain.model.TraceSpan;

import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;

public final class TraceSnapshotFixtures {

    private TraceSnapshotFixtures() {}

    public static TraceSpan aRootSpan() {
        return new TraceSpan("span-1", null, "POST /api/v1/payments",
                "payment-orchestrator",
                Instant.parse("2026-03-08T09:45:00Z"), 2700000L, "OK", null);
    }

    public static TraceSpan aComplianceSpan() {
        return new TraceSpan("span-2", "span-1", "ComplianceCheck",
                "compliance-travel-rule",
                Instant.parse("2026-03-08T09:50:00Z"), 60000L, "OK", null);
    }

    public static TraceSpan anErrorSpan() {
        return new TraceSpan("span-3", "span-1", "BlockchainSubmit",
                "blockchain-custody",
                Instant.parse("2026-03-08T10:15:00Z"), 0L, "ERROR",
                "Transaction pending — awaiting confirmation");
    }

    public static TraceSnapshot aTraceSnapshot() {
        return new TraceSnapshot(
                PAYMENT_ID,
                "trace-abc-123",
                3,
                2700000L,
                List.of(aRootSpan(), aComplianceSpan(), anErrorSpan()));
    }
}
