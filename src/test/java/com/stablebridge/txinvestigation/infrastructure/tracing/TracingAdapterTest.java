package com.stablebridge.txinvestigation.infrastructure.tracing;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
class TracingAdapterTest {

    private TracingAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new TracingAdapter(webClient);
    }

    @Test
    void shouldFetchTrace() {
        // given
        stubFor(get(urlPathEqualTo("/api/traces"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": [{
                                    "traceID": "trace-001",
                                    "durationMs": 2700000,
                                    "spans": [{
                                      "spanID": "span-1",
                                      "parentSpanID": null,
                                      "operationName": "POST /api/v1/payments",
                                      "serviceName": "payment-orchestrator",
                                      "startTime": "2026-03-08T09:45:00Z",
                                      "durationMs": 2700000,
                                      "status": "OK",
                                      "errorMessage": null
                                    }, {
                                      "spanID": "span-2",
                                      "parentSpanID": "span-1",
                                      "operationName": "BlockchainSubmit",
                                      "serviceName": "blockchain-custody",
                                      "startTime": "2026-03-08T10:15:00Z",
                                      "durationMs": 0,
                                      "status": "ERROR",
                                      "errorMessage": "Transaction pending"
                                    }]
                                  }]
                                }
                                """)));

        // when
        var result = adapter.fetchTrace("PAY-001");

        // then
        assertThat(result.traceId()).isEqualTo("trace-001");
        assertThat(result.totalSpans()).isEqualTo(2);
        assertThat(result.durationMs()).isEqualTo(2700000L);
        assertThat(result.spans().get(1).status()).isEqualTo("ERROR");
    }

    @Test
    void shouldReturnEmptySnapshotWhenNoTracesFound() {
        // given
        stubFor(get(urlPathEqualTo("/api/traces"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                { "data": [] }
                                """)));

        // when
        var result = adapter.fetchTrace("PAY-001");

        // then
        assertThat(result.totalSpans()).isZero();
        assertThat(result.spans()).isEmpty();
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(get(urlPathEqualTo("/api/traces"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.fetchTrace("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("tracing");
    }
}
