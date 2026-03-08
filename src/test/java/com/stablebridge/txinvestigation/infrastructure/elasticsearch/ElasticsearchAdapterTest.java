package com.stablebridge.txinvestigation.infrastructure.elasticsearch;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.stablebridge.txinvestigation.domain.model.LogLevel;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class ElasticsearchAdapterTest {

    private ElasticsearchAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new ElasticsearchAdapter(webClient);
    }

    @Test
    void shouldSearchErrorLogs() {
        // given
        stubFor(post(urlPathEqualTo("/payment-logs-*/_search"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "hits": {
                                    "total": { "value": 2 },
                                    "hits": [{
                                      "_source": {
                                        "@timestamp": "2026-03-08T10:15:30Z",
                                        "level": "WARN",
                                        "service": "blockchain-custody",
                                        "message": "Timeout approaching SLA",
                                        "traceId": "trace-001"
                                      }
                                    }, {
                                      "_source": {
                                        "@timestamp": "2026-03-08T10:24:10Z",
                                        "level": "ERROR",
                                        "service": "blockchain-custody",
                                        "message": "Gas price spike",
                                        "traceId": "trace-001",
                                        "stackTrace": "java.lang.RuntimeException: Gas failed"
                                      }
                                    }]
                                  }
                                }
                                """)));

        // when
        var result = adapter.searchErrorLogs("PAY-001");

        // then
        assertThat(result.paymentId()).isEqualTo("PAY-001");
        assertThat(result.totalHits()).isEqualTo(2);
        assertThat(result.entries()).hasSize(2);
        assertThat(result.entries().get(0).level()).isEqualTo(LogLevel.WARN);
        assertThat(result.entries().get(1).stackTrace()).contains("Gas failed");
    }

    @Test
    void shouldReturnEmptySnapshotWhenIndexNotFound() {
        // given
        stubFor(post(urlPathEqualTo("/payment-logs-*/_search"))
                .willReturn(aResponse().withStatus(404)));

        // when
        var result = adapter.searchErrorLogs("PAY-001");

        // then
        assertThat(result.totalHits()).isZero();
        assertThat(result.entries()).isEmpty();
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(post(urlPathEqualTo("/payment-logs-*/_search"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.searchErrorLogs("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("elasticsearch");
    }
}
