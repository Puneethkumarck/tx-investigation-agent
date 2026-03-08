package com.stablebridge.txinvestigation.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.stablebridge.txinvestigation.domain.model.BlockchainSnapshot;
import com.stablebridge.txinvestigation.domain.model.CompletedInvestigation;
import com.stablebridge.txinvestigation.domain.model.ComplianceSnapshot;
import com.stablebridge.txinvestigation.domain.model.InvestigationQuery;
import com.stablebridge.txinvestigation.domain.model.InvestigationReport;
import com.stablebridge.txinvestigation.domain.model.LedgerSnapshot;
import com.stablebridge.txinvestigation.domain.model.PaymentState;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import com.stablebridge.txinvestigation.domain.service.ReportFormatter;
import lombok.RequiredArgsConstructor;

import static com.stablebridge.txinvestigation.agent.InvestigationPersonas.SENIOR_INVESTIGATOR;

@Agent(description = "Investigate a payment's lifecycle across all services and produce a report")
@RequiredArgsConstructor
public class InvestigationAgent {

    private final PaymentStateProvider paymentStateProvider;
    private final ComplianceStateProvider complianceStateProvider;
    private final BlockchainStateProvider blockchainStateProvider;
    private final LedgerStateProvider ledgerStateProvider;
    private final ReportFormatter reportFormatter;

    @Action
    public InvestigationQuery parseQuery(UserInput userInput, Ai ai) {
        return ai
                .withAutoLlm()
                .creating(InvestigationQuery.class)
                .fromPrompt("""
                        Extract the payment investigation query from this input.
                        The user may provide a payment ID like "PAY-abc-123"
                        or say "investigate payment PAY-abc-123 for merchant MCH-001".
                        Return paymentId (required), merchantId (optional), corridor (optional).

                        User input: %s
                        """.formatted(userInput.getContent()));
    }

    @Action
    public PaymentState fetchPaymentState(InvestigationQuery query) {
        return paymentStateProvider.fetchPaymentState(query.paymentId());
    }

    @Action
    public ComplianceSnapshot fetchComplianceStatus(InvestigationQuery query) {
        return complianceStateProvider.fetchComplianceStatus(query.paymentId());
    }

    @Action
    public BlockchainSnapshot fetchBlockchainStatus(InvestigationQuery query) {
        return blockchainStateProvider.fetchBlockchainStatus(query.paymentId());
    }

    @Action
    public LedgerSnapshot fetchLedgerEntries(InvestigationQuery query) {
        return ledgerStateProvider.fetchLedgerEntries(query.paymentId());
    }

    @Action
    public InvestigationReport analyzeTimeline(
            PaymentState paymentState,
            ComplianceSnapshot complianceSnapshot,
            BlockchainSnapshot blockchainSnapshot,
            LedgerSnapshot ledgerSnapshot,
            Ai ai) {

        return ai
                .withAutoLlm()
                .withPromptContributor(SENIOR_INVESTIGATOR)
                .creating(InvestigationReport.class)
                .fromPrompt("""
                        Analyze the following payment data from multiple services and produce
                        an investigation report with timeline, root cause, findings, and recommendations.

                        ## Payment State (Orchestrator)
                        Payment ID: %s
                        Status: %s
                        Saga Step: %s
                        Workflow ID: %s
                        Events: %s

                        ## Compliance Status
                        Screening: %s
                        Travel Rule: %s
                        Risk Score: %.2f
                        Decisions: %s

                        ## Blockchain Status
                        TX Hash: %s
                        Chain: %s
                        Confirmations: %d
                        Amount: %s %s
                        Status: %s

                        ## Ledger Status
                        Entries: %s
                        Net Position: %s
                        Settlement: %s

                        ## Instructions
                        1. Build a chronological timeline of events across all services
                        2. Identify the root cause if the payment is stuck or failed
                        3. List findings with severity (CRITICAL, HIGH, MEDIUM, LOW, INFO)
                           and category (STUCK_PAYMENT, COMPLIANCE_BLOCK, BLOCKCHAIN_DELAY,
                           SETTLEMENT_MISMATCH, SLA_BREACH, RECONCILIATION_GAP)
                        4. Provide actionable recommendations
                        5. Set overall severity based on the most critical finding
                        """.formatted(
                        paymentState.paymentId(),
                        paymentState.status(),
                        paymentState.sagaStep(),
                        paymentState.workflowId(),
                        paymentState.events(),
                        complianceSnapshot.screeningResult(),
                        complianceSnapshot.travelRuleStatus(),
                        complianceSnapshot.riskScore(),
                        complianceSnapshot.decisions(),
                        blockchainSnapshot.txHash(),
                        blockchainSnapshot.chain(),
                        blockchainSnapshot.confirmations(),
                        blockchainSnapshot.amount(),
                        blockchainSnapshot.currency(),
                        blockchainSnapshot.status(),
                        ledgerSnapshot.entries(),
                        ledgerSnapshot.netPosition(),
                        ledgerSnapshot.settlementStatus()));
    }

    @AchievesGoal(
            description = "A completed investigation report has been produced for the payment",
            export = @Export(remote = true, name = "paymentInvestigation")
    )
    @Action
    public CompletedInvestigation formatReport(
            InvestigationQuery query,
            PaymentState paymentState,
            ComplianceSnapshot complianceSnapshot,
            BlockchainSnapshot blockchainSnapshot,
            LedgerSnapshot ledgerSnapshot,
            InvestigationReport report) {

        var formattedBody = reportFormatter.format(report, paymentState);

        return new CompletedInvestigation(
                query,
                paymentState,
                complianceSnapshot,
                blockchainSnapshot,
                ledgerSnapshot,
                report,
                formattedBody);
    }
}
