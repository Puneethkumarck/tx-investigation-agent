package com.stablebridge.txinvestigation.infrastructure.orchestrator;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.PaymentStatus;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class OrchestratorAdapterTest {

    private OrchestratorAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new OrchestratorAdapter(webClient);
    }

    @Test
    void shouldFetchPaymentState() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/payments/PAY-001"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentId": "PAY-001",
                                  "status": "BLOCKCHAIN_PENDING",
                                  "sagaStep": "BLOCKCHAIN_SUBMIT",
                                  "workflowId": "wf-123",
                                  "events": [{
                                    "step": "COMPLIANCE_CHECK",
                                    "status": "COMPLETED",
                                    "timestamp": "2026-03-08T10:00:00Z",
                                    "detail": "All checks passed"
                                  }],
                                  "createdAt": "2026-03-08T09:45:00Z",
                                  "updatedAt": "2026-03-08T10:30:00Z"
                                }
                                """)));

        // when
        var result = adapter.fetchPaymentState("PAY-001");

        // then
        assertThat(result.paymentId()).isEqualTo("PAY-001");
        assertThat(result.status()).isEqualTo(PaymentStatus.BLOCKCHAIN_PENDING);
        assertThat(result.events()).hasSize(1);
    }

    @Test
    void shouldThrowPaymentNotFoundOn404() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/payments/PAY-MISSING"))
                .willReturn(aResponse().withStatus(404)));

        // then
        assertThatThrownBy(() -> adapter.fetchPaymentState("PAY-MISSING"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("PAY-MISSING");
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/payments/PAY-001"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.fetchPaymentState("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("orchestrator");
    }
}
