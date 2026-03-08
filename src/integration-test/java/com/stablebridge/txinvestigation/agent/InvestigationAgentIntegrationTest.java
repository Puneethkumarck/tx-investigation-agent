package com.stablebridge.txinvestigation.agent;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.stablebridge.txinvestigation.config.TestJacksonConfig;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.LogSearchProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import com.stablebridge.txinvestigation.domain.port.TraceProvider;
import com.stablebridge.txinvestigation.domain.port.WorkflowHistoryProvider;
import com.stablebridge.txinvestigation.domain.service.ReportFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Import;

import static com.stablebridge.txinvestigation.fixtures.BlockchainSnapshotFixtures.aBlockchainSnapshot;
import static com.stablebridge.txinvestigation.fixtures.CompletedInvestigationFixtures.aCompletedInvestigation;
import static com.stablebridge.txinvestigation.fixtures.ComplianceSnapshotFixtures.aComplianceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.anInvestigationQuery;
import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.anInvestigationReport;
import static com.stablebridge.txinvestigation.fixtures.LedgerSnapshotFixtures.aLedgerSnapshot;
import static com.stablebridge.txinvestigation.fixtures.LogSnapshotFixtures.aLogSnapshot;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static com.stablebridge.txinvestigation.fixtures.TraceSnapshotFixtures.aTraceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.WorkflowSnapshotFixtures.aWorkflowSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@Import(TestJacksonConfig.class)
class InvestigationAgentIntegrationTest extends EmbabelMockitoIntegrationTest {

    private PaymentStateProvider paymentStateProvider;
    private ComplianceStateProvider complianceStateProvider;
    private BlockchainStateProvider blockchainStateProvider;
    private LedgerStateProvider ledgerStateProvider;
    private WorkflowHistoryProvider workflowHistoryProvider;
    private LogSearchProvider logSearchProvider;
    private TraceProvider traceProvider;

    private final ReportFormatter reportFormatter = new ReportFormatter();
    private InvestigationAgent agent;

    @BeforeEach
    void setUpMocks() {
        paymentStateProvider = Mockito.mock(PaymentStateProvider.class);
        complianceStateProvider = Mockito.mock(ComplianceStateProvider.class);
        blockchainStateProvider = Mockito.mock(BlockchainStateProvider.class);
        ledgerStateProvider = Mockito.mock(LedgerStateProvider.class);
        workflowHistoryProvider = Mockito.mock(WorkflowHistoryProvider.class);
        logSearchProvider = Mockito.mock(LogSearchProvider.class);
        traceProvider = Mockito.mock(TraceProvider.class);

        agent = new InvestigationAgent(
                paymentStateProvider,
                complianceStateProvider,
                blockchainStateProvider,
                ledgerStateProvider,
                workflowHistoryProvider,
                logSearchProvider,
                traceProvider,
                reportFormatter);
    }

    @Test
    void shouldAssembleCompletedInvestigationFromAllProviders() {
        // given
        var query = anInvestigationQuery();
        var paymentState = aPaymentState();
        var complianceSnapshot = aComplianceSnapshot();
        var blockchainSnapshot = aBlockchainSnapshot();
        var ledgerSnapshot = aLedgerSnapshot();
        var workflowSnapshot = aWorkflowSnapshot();
        var logSnapshot = aLogSnapshot();
        var traceSnapshot = aTraceSnapshot();
        var report = anInvestigationReport();

        // when
        var result = agent.formatReport(
                query, paymentState, complianceSnapshot, blockchainSnapshot,
                ledgerSnapshot, workflowSnapshot, logSnapshot, traceSnapshot, report);

        // then
        var expected = aCompletedInvestigation();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("formattedBody")
                .isEqualTo(expected);
    }

    @Test
    void shouldFetchDataThroughAllSevenProviderPorts() {
        // given
        var query = anInvestigationQuery();
        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(aPaymentState());
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(aComplianceSnapshot());
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(aBlockchainSnapshot());
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(aLedgerSnapshot());
        given(workflowHistoryProvider.fetchWorkflowHistory(PAYMENT_ID)).willReturn(aWorkflowSnapshot());
        given(logSearchProvider.searchErrorLogs(PAYMENT_ID)).willReturn(aLogSnapshot());
        given(traceProvider.fetchTrace(PAYMENT_ID)).willReturn(aTraceSnapshot());

        // when
        agent.fetchPaymentState(query);
        agent.fetchComplianceStatus(query);
        agent.fetchBlockchainStatus(query);
        agent.fetchLedgerEntries(query);
        agent.fetchWorkflowHistory(query);
        agent.searchErrorLogs(query);
        agent.fetchTrace(query);

        // then
        then(paymentStateProvider).should().fetchPaymentState(PAYMENT_ID);
        then(complianceStateProvider).should().fetchComplianceStatus(PAYMENT_ID);
        then(blockchainStateProvider).should().fetchBlockchainStatus(PAYMENT_ID);
        then(ledgerStateProvider).should().fetchLedgerEntries(PAYMENT_ID);
        then(workflowHistoryProvider).should().fetchWorkflowHistory(PAYMENT_ID);
        then(logSearchProvider).should().searchErrorLogs(PAYMENT_ID);
        then(traceProvider).should().fetchTrace(PAYMENT_ID);
    }

    @Test
    void shouldProduceFormattedReportWithTimeline() {
        // given
        var query = anInvestigationQuery();
        var paymentState = aPaymentState();
        var report = anInvestigationReport();

        // when
        var result = agent.formatReport(
                query, paymentState, aComplianceSnapshot(), aBlockchainSnapshot(),
                aLedgerSnapshot(), aWorkflowSnapshot(), aLogSnapshot(), aTraceSnapshot(),
                report);

        // then
        assertThat(result.formattedBody())
                .contains("PAY-abc-123")
                .contains("BLOCKCHAIN_PENDING")
                .contains("Timeline")
                .contains("Findings");
    }
}
