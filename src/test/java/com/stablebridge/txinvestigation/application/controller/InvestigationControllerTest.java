package com.stablebridge.txinvestigation.application.controller;

import com.stablebridge.txinvestigation.domain.model.InvestigationReport;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.stablebridge.txinvestigation.fixtures.BlockchainSnapshotFixtures.aBlockchainSnapshot;
import static com.stablebridge.txinvestigation.fixtures.ComplianceSnapshotFixtures.aComplianceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.InvestigationQueryFixtures.PAYMENT_ID;
import static com.stablebridge.txinvestigation.fixtures.InvestigationReportFixtures.anInvestigationReport;
import static com.stablebridge.txinvestigation.fixtures.LedgerSnapshotFixtures.aLedgerSnapshot;
import static com.stablebridge.txinvestigation.fixtures.LogSnapshotFixtures.aLogSnapshot;
import static com.stablebridge.txinvestigation.fixtures.PaymentStateFixtures.aPaymentState;
import static com.stablebridge.txinvestigation.fixtures.TraceSnapshotFixtures.aTraceSnapshot;
import static com.stablebridge.txinvestigation.fixtures.WorkflowSnapshotFixtures.aWorkflowSnapshot;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class InvestigationControllerTest {

    @Mock private PaymentStateProvider paymentStateProvider;
    @Mock private ComplianceStateProvider complianceStateProvider;
    @Mock private BlockchainStateProvider blockchainStateProvider;
    @Mock private LedgerStateProvider ledgerStateProvider;
    @Mock private WorkflowHistoryProvider workflowHistoryProvider;
    @Mock private LogSearchProvider logSearchProvider;
    @Mock private TraceProvider traceProvider;
    @Mock private ChatClient.Builder chatClientBuilder;

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        var chatClient = mock(ChatClient.class);
        var promptRequest = mock(ChatClient.ChatClientRequestSpec.class);
        var callResponse = mock(ChatClient.CallResponseSpec.class);

        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        lenient().when(chatClient.prompt()).thenReturn(promptRequest);
        lenient().when(promptRequest.user(anyString())).thenReturn(promptRequest);
        lenient().when(promptRequest.call()).thenReturn(callResponse);
        lenient().when(callResponse.entity(InvestigationReport.class)).thenReturn(anInvestigationReport());

        var controller = new InvestigationController(
                paymentStateProvider,
                complianceStateProvider,
                blockchainStateProvider,
                ledgerStateProvider,
                workflowHistoryProvider,
                logSearchProvider,
                traceProvider,
                new ReportFormatter(),
                chatClientBuilder);
        webClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void shouldReturnInvestigationResponse() {
        // given
        given(paymentStateProvider.fetchPaymentState(PAYMENT_ID)).willReturn(aPaymentState());
        given(complianceStateProvider.fetchComplianceStatus(PAYMENT_ID)).willReturn(aComplianceSnapshot());
        given(blockchainStateProvider.fetchBlockchainStatus(PAYMENT_ID)).willReturn(aBlockchainSnapshot());
        given(ledgerStateProvider.fetchLedgerEntries(PAYMENT_ID)).willReturn(aLedgerSnapshot());
        given(workflowHistoryProvider.fetchWorkflowHistory(PAYMENT_ID)).willReturn(aWorkflowSnapshot());
        given(logSearchProvider.searchErrorLogs(PAYMENT_ID)).willReturn(aLogSnapshot());
        given(traceProvider.fetchTrace(PAYMENT_ID)).willReturn(aTraceSnapshot());

        // when / then
        webClient.post()
                .uri("/api/v1/investigations")
                .bodyValue(new InvestigationRequest(PAYMENT_ID, null))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.paymentId").isEqualTo(PAYMENT_ID)
                .jsonPath("$.status").isEqualTo("BLOCKCHAIN_PENDING")
                .jsonPath("$.severity").isEqualTo("HIGH")
                .jsonPath("$.rootCause").isNotEmpty()
                .jsonPath("$.workflowStatus").isEqualTo("RUNNING")
                .jsonPath("$.errorLogCount").isEqualTo(2)
                .jsonPath("$.traceId").isEqualTo("trace-abc-123");
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
