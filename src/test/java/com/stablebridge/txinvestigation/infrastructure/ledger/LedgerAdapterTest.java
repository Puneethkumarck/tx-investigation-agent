package com.stablebridge.txinvestigation.infrastructure.ledger;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.stablebridge.txinvestigation.domain.model.Direction;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class LedgerAdapterTest {

    private LedgerAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new LedgerAdapter(webClient);
    }

    @Test
    void shouldFetchLedgerEntries() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/ledger/payments/PAY-001"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentId": "PAY-001",
                                  "entries": [{
                                    "entryId": "LE-001",
                                    "account": "MERCHANT_USD",
                                    "direction": "DEBIT",
                                    "amount": "1000.00",
                                    "currency": "USD",
                                    "timestamp": "2026-03-08T10:15:00Z"
                                  }],
                                  "netPosition": "0",
                                  "settlementStatus": "PENDING"
                                }
                                """)));

        // when
        var result = adapter.fetchLedgerEntries("PAY-001");

        // then
        assertThat(result.paymentId()).isEqualTo("PAY-001");
        assertThat(result.entries()).hasSize(1);
        assertThat(result.entries().getFirst().direction()).isEqualTo(Direction.DEBIT);
        assertThat(result.netPosition()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowPaymentNotFoundOn404() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/ledger/payments/PAY-MISSING"))
                .willReturn(aResponse().withStatus(404)));

        // then
        assertThatThrownBy(() -> adapter.fetchLedgerEntries("PAY-MISSING"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/ledger/payments/PAY-001"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.fetchLedgerEntries("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("ledger");
    }
}
