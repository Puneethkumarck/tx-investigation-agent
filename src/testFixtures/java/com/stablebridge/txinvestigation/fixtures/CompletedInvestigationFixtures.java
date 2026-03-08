package com.stablebridge.txinvestigation.fixtures;

import com.stablebridge.txinvestigation.domain.model.CompletedInvestigation;

import static com.stablebridge.txinvestigation.fixtures.BlockchainSnapshotFixtures.aBlockchainSnapshot;
import static com.stablebridge.txinvestigation.fixtures.ComplianceSnapshotFixtures.aComplianceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.anInvestigationQuery;
import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.anInvestigationReport;
import static com.stablebridge.txinvestigation.fixtures.LedgerSnapshotFixtures.aLedgerSnapshot;
import static com.stablebridge.txinvestigation.fixtures.LogSnapshotFixtures.aLogSnapshot;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static com.stablebridge.txinvestigation.fixtures.TraceSnapshotFixtures.aTraceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.WorkflowSnapshotFixtures.aWorkflowSnapshot;

public final class CompletedInvestigationFixtures {

    private CompletedInvestigationFixtures() {}

    public static CompletedInvestigation aCompletedInvestigation() {
        return new CompletedInvestigation(
                anInvestigationQuery(),
                aPaymentState(),
                aComplianceSnapshot(),
                aBlockchainSnapshot(),
                aLedgerSnapshot(),
                aWorkflowSnapshot(),
                aLogSnapshot(),
                aTraceSnapshot(),
                anInvestigationReport(),
                "## Investigation Report: PAY-abc-123\n...");
    }
}
