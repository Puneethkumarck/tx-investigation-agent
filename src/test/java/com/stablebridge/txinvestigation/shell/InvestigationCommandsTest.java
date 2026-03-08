package com.stablebridge.txinvestigation.shell;

import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import com.stablebridge.txinvestigation.domain.service.ReportFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.stablebridge.txinvestigation.fixtures.BlockchainSnapshotFixtures.aBlockchainSnapshot;
import static com.stablebridge.txinvestigation.fixtures.ComplianceSnapshotFixtures.aComplianceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;
import static com.stablebridge.txinvestigation.fixtures.LedgerSnapshotFixtures.aLedgerSnapshot;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class InvestigationCommandsTest {

    @Mock private PaymentStateProvider paymentStateProvider;
    @Mock private ComplianceStateProvider complianceStateProvider;
    @Mock private BlockchainStateProvider blockchainStateProvider;
    @Mock private LedgerStateProvider ledgerStateProvider;

    private InvestigationCommands commands;

    @BeforeEach
    void setUp() {
        commands = new InvestigationCommands(
                paymentStateProvider,
                complianceStateProvider,
                blockchainStateProvider,
                ledgerStateProvider,
                new ReportFormatter());
    }

    @Test
    void shouldInvestigatePaymentAndDisplaySummary() {
        // given
        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(aPaymentState());
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(aComplianceSnapshot());
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(aBlockchainSnapshot());
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(aLedgerSnapshot());

        // when
        var result = commands.investigate(PAYMENT_ID, "");

        // then
        assertThat(result)
                .contains("Investigation: PAY-abc-123")
                .contains("BLOCKCHAIN_PENDING")
                .contains("CLEAR")
                .contains("PENDING")
                .contains("Timeline");

        then(paymentStateProvider).should().fetchPaymentState(PAYMENT_ID);
        then(complianceStateProvider).should().fetchComplianceStatus(PAYMENT_ID);
        then(blockchainStateProvider).should().fetchBlockchainStatus(PAYMENT_ID);
        then(ledgerStateProvider).should().fetchLedgerEntries(PAYMENT_ID);
    }
}
