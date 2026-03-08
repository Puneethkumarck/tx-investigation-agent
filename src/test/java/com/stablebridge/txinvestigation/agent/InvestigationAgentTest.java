package com.stablebridge.txinvestigation.agent;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class InvestigationAgentTest {

    @Mock private PaymentStateProvider paymentStateProvider;
    @Mock private ComplianceStateProvider complianceStateProvider;
    @Mock private BlockchainStateProvider blockchainStateProvider;
    @Mock private LedgerStateProvider ledgerStateProvider;
    @Mock private WorkflowHistoryProvider workflowHistoryProvider;
    @Mock private LogSearchProvider logSearchProvider;
    @Mock private TraceProvider traceProvider;
    @Mock private ReportFormatter reportFormatter;

    private InvestigationAgent agent;

    @BeforeEach
    void setUp() {
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
    void shouldFetchPaymentStateThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aPaymentState();
        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.fetchPaymentState(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(paymentStateProvider).should().fetchPaymentState(PAYMENT_ID);
    }

    @Test
    void shouldFetchComplianceStatusThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aComplianceSnapshot();
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.fetchComplianceStatus(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(complianceStateProvider).should().fetchComplianceStatus(PAYMENT_ID);
    }

    @Test
    void shouldFetchBlockchainStatusThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aBlockchainSnapshot();
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.fetchBlockchainStatus(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(blockchainStateProvider).should().fetchBlockchainStatus(PAYMENT_ID);
    }

    @Test
    void shouldFetchLedgerEntriesThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aLedgerSnapshot();
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.fetchLedgerEntries(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(ledgerStateProvider).should().fetchLedgerEntries(PAYMENT_ID);
    }

    @Test
    void shouldFetchWorkflowHistoryThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aWorkflowSnapshot();
        given(workflowHistoryProvider.fetchWorkflowHistory(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.fetchWorkflowHistory(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(workflowHistoryProvider).should().fetchWorkflowHistory(PAYMENT_ID);
    }

    @Test
    void shouldSearchErrorLogsThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aLogSnapshot();
        given(logSearchProvider.searchErrorLogs(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.searchErrorLogs(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(logSearchProvider).should().searchErrorLogs(PAYMENT_ID);
    }

    @Test
    void shouldFetchTraceThroughPort() {
        // given
        var query = anInvestigationQuery();
        var expected = aTraceSnapshot();
        given(traceProvider.fetchTrace(PAYMENT_ID)).willReturn(expected);

        // when
        var result = agent.fetchTrace(query);

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        then(traceProvider).should().fetchTrace(PAYMENT_ID);
    }

    @Test
    void shouldFormatReportAndAssembleCompletedInvestigation() {
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
        var expectedBody = "## Investigation Report: PAY-abc-123\n...";
        given(reportFormatter.format(report, paymentState)).willReturn(expectedBody);

        // when
        var result = agent.formatReport(
                query, paymentState, complianceSnapshot, blockchainSnapshot,
                ledgerSnapshot, workflowSnapshot, logSnapshot, traceSnapshot, report);

        // then
        var expected = aCompletedInvestigation();
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
        then(reportFormatter).should().format(report, paymentState);
    }
}
