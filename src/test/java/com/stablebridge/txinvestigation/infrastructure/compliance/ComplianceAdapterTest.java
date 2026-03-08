package com.stablebridge.txinvestigation.infrastructure.compliance;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.ScreeningResult;
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
class ComplianceAdapterTest {

    private ComplianceAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new ComplianceAdapter(webClient);
    }

    @Test
    void shouldFetchComplianceStatus() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/compliance/payments/PAY-001"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentId": "PAY-001",
                                  "screeningResult": "CLEAR",
                                  "travelRuleStatus": "COMPLETED",
                                  "riskScore": 0.15,
                                  "decisions": [{
                                    "checkType": "SANCTIONS_SCREENING",
                                    "result": "CLEAR",
                                    "provider": "chainalysis",
                                    "timestamp": "2026-03-08T10:01:00Z",
                                    "detail": "No matches found"
                                  }]
                                }
                                """)));

        // when
        var result = adapter.fetchComplianceStatus("PAY-001");

        // then
        assertThat(result.paymentId()).isEqualTo("PAY-001");
        assertThat(result.screeningResult()).isEqualTo(ScreeningResult.CLEAR);
        assertThat(result.decisions()).hasSize(1);
    }

    @Test
    void shouldThrowPaymentNotFoundOn404() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/compliance/payments/PAY-MISSING"))
                .willReturn(aResponse().withStatus(404)));

        // then
        assertThatThrownBy(() -> adapter.fetchComplianceStatus("PAY-MISSING"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/compliance/payments/PAY-001"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.fetchComplianceStatus("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("compliance");
    }
}
