package com.stablebridge.txinvestigation.infrastructure.temporal;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
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
class TemporalAdapterTest {

    private TemporalAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new TemporalAdapter(webClient);
    }

    @Test
    void shouldFetchWorkflowHistory() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/workflows/PAY-001"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentId": "PAY-001",
                                  "workflowId": "wf-001",
                                  "workflowType": "PaymentWorkflow",
                                  "status": "RUNNING",
                                  "startTime": "2026-03-08T09:45:00Z",
                                  "closeTime": null,
                                  "attemptCount": 1,
                                  "taskQueue": "payment-task-queue",
                                  "events": [{
                                    "eventId": 1,
                                    "eventType": "WorkflowExecutionStarted",
                                    "timestamp": "2026-03-08T09:45:00Z",
                                    "detail": "Workflow started"
                                  }, {
                                    "eventId": 2,
                                    "eventType": "ActivityTaskCompleted",
                                    "timestamp": "2026-03-08T09:50:00Z",
                                    "activityType": "ComplianceCheck",
                                    "detail": "Compliance check passed"
                                  }]
                                }
                                """)));

        // when
        var result = adapter.fetchWorkflowHistory("PAY-001");

        // then
        assertThat(result.paymentId()).isEqualTo("PAY-001");
        assertThat(result.workflowId()).isEqualTo("wf-001");
        assertThat(result.status()).isEqualTo("RUNNING");
        assertThat(result.events()).hasSize(2);
    }

    @Test
    void shouldThrowPaymentNotFoundOn404() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/workflows/PAY-MISSING"))
                .willReturn(aResponse().withStatus(404)));

        // then
        assertThatThrownBy(() -> adapter.fetchWorkflowHistory("PAY-MISSING"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("PAY-MISSING");
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/workflows/PAY-001"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.fetchWorkflowHistory("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("temporal");
    }
}
