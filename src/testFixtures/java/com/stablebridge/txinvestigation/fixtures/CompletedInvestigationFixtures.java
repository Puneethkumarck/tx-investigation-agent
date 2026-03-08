package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.CompletedInvestigation;

import static com.stablebridge.txinvestigation.fixtures.BlockchainSnapshotFixtures.aBlockchainSnapshot;
import static com.stablebridge.txinvestigation.fixtures.ComplianceSnapshotFixtures.aComplianceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.anInvestigationQuery;
import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.anInvestigationReport;
import static com.stablebridge.txinvestigation.fixtures.LedgerSnapshotFixtures.aLedgerSnapshot;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;

public final class CompletedInvestigationFixtures {

    private CompletedInvestigationFixtures() {}

    public static CompletedInvestigation aCompletedInvestigation() {
        return new CompletedInvestigation(
                anInvestigationQuery(),
                aPaymentState(),
                aComplianceSnapshot(),
                aBlockchainSnapshot(),
                aLedgerSnapshot(),
                anInvestigationReport(),
                "## Investigation Report: PAY-abc-123\n...");
    }
}
