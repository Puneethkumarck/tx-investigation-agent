package com.stablebridge.txinvestigation.application.controller;

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
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.stablebridge.txinvestigation.fixtures.BlockchainSnapshotFixtures.aBlockchainSnapshot;
import static com.stablebridge.txinvestigation.fixtures.ComplianceSnapshotFixtures.aComplianceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;
import static com.stablebridge.txinvestigation.fixtures.LedgerSnapshotFixtures.aLedgerSnapshot;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InvestigationControllerTest {

    @Mock private PaymentStateProvider paymentStateProvider;
    @Mock private ComplianceStateProvider complianceStateProvider;
    @Mock private BlockchainStateProvider blockchainStateProvider;
    @Mock private LedgerStateProvider ledgerStateProvider;

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        var controller = new InvestigationController(
                paymentStateProvider,
                complianceStateProvider,
                blockchainStateProvider,
                ledgerStateProvider,
                new ReportFormatter());
        webClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void shouldReturnInvestigationResponse() {
        // given
        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(aPaymentState());
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(aComplianceSnapshot());
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(aBlockchainSnapshot());
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(aLedgerSnapshot());

        // when / then
        webClient.post()
                .uri("/api/v1/investigations")
                .bodyValue(new InvestigationRequest(PAYMENT_ID, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo(PAYMENT_ID)
                .jsonPath("$.status").isEqualTo("BLOCKCHAIN_PENDING");
    }

    @Test
    void shouldReturn400ForMissingPaymentId() {
        webClient.post()
                .uri("/api/v1/investigations")
                .bodyValue(new InvestigationRequest("", null))
                .exchange()
                .expectStatus().isBadRequest();
    }
}
