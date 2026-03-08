package com.stablebridge.txinvestigation.agent;

import com.embabel.agent.test.integration.EmbabelMockitoIntegrationTest;
import com.stablebridge.txinvestigation.config.TestJacksonConfig;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
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
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@Import(TestJacksonConfig.class)
class InvestigationAgentIntegrationTest extends EmbabelMockitoIntegrationTest {

    private PaymentStateProvider paymentStateProvider;
    private ComplianceStateProvider complianceStateProvider;
    private BlockchainStateProvider blockchainStateProvider;
    private LedgerStateProvider ledgerStateProvider;

    private final ReportFormatter reportFormatter = new ReportFormatter();
    private InvestigationAgent agent;

    @BeforeEach
    void setUpMocks() {
        paymentStateProvider = Mockito.mock(PaymentStateProvider.class);
        complianceStateProvider = Mockito.mock(ComplianceStateProvider.class);
        blockchainStateProvider = Mockito.mock(BlockchainStateProvider.class);
        ledgerStateProvider = Mockito.mock(LedgerStateProvider.class);

        agent = new InvestigationAgent(
                paymentStateProvider,
                complianceStateProvider,
                blockchainStateProvider,
                ledgerStateProvider,
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
        var report = anInvestigationReport();

        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(paymentState);
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(complianceSnapshot);
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(blockchainSnapshot);
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(ledgerSnapshot);

        // when
        var result = agent.formatReport(
                query, paymentState, complianceSnapshot,
                blockchainSnapshot, ledgerSnapshot, report);

        // then
        var expected = aCompletedInvestigation();
        assertThat(result)
                .usingRecursiveComparison()
                .ignoringFields("formattedBody")
                .isEqualTo(expected);
    }

    @Test
    void shouldFetchDataThroughAllFourProviderPorts() {
        // given
        var query = anInvestigationQuery();
        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(aPaymentState());
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(aComplianceSnapshot());
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(aBlockchainSnapshot());
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(aLedgerSnapshot());

        // when
        agent.fetchPaymentState(query);
        agent.fetchComplianceStatus(query);
        agent.fetchBlockchainStatus(query);
        agent.fetchLedgerEntries(query);

        // then
        then(paymentStateProvider).should().fetchPaymentState(PAYMENT_ID);
        then(complianceStateProvider).should().fetchComplianceStatus(PAYMENT_ID);
        then(blockchainStateProvider).should().fetchBlockchainStatus(PAYMENT_ID);
        then(ledgerStateProvider).should().fetchLedgerEntries(PAYMENT_ID);
    }

    @Test
    void shouldProduceFormattedReportWithTimeline() {
        // given
        var query = anInvestigationQuery();
        var paymentState = aPaymentState();
        var report = anInvestigationReport();

        // when
        var result = agent.formatReport(
                query, paymentState, aComplianceSnapshot(),
                aBlockchainSnapshot(), aLedgerSnapshot(), report);

        // then
        assertThat(result.formattedBody())
                .contains("PAY-abc-123")
                .contains("BLOCKCHAIN_PENDING")
                .contains("Timeline")
                .contains("Findings");
    }
}
