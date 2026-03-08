package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.ComplianceDecision;
import com.stablebridge.txinvestigation.domain.model.ComplianceSnapshot;

import java.time.Instant;
import java.util.List;

import static com.stablebridge.txinvestigation.domain.model.ScreeningResult.CLEAR;

public final class ComplianceSnapshotFixtures {

    private ComplianceSnapshotFixtures() {}

    public static ComplianceDecision aComplianceDecision() {
        return new ComplianceDecision(
                "SANCTIONS_SCREENING",
                "CLEAR",
                "chainalysis",
                Instant.parse("2026-03-08T10:01:00Z"),
                "No matches found");
    }

    public static ComplianceSnapshot aComplianceSnapshot() {
        return new ComplianceSnapshot(
                InvestigationQueryFixtures.PAYMENT_ID,
                CLEAR,
                "COMPLETED",
                0.15,
                List.of(
                        aComplianceDecision(),
                        new ComplianceDecision("TRAVEL_RULE", "COMPLETED", "notabene",
                                Instant.parse("2026-03-08T10:02:00Z"), "IVMS101 exchanged")
                ));
    }
}
