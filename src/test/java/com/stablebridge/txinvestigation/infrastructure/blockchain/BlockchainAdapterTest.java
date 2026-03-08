package com.stablebridge.txinvestigation.infrastructure.blockchain;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
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
class BlockchainAdapterTest {

    private BlockchainAdapter adapter;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmInfo) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        var webClient = WebClient.builder()
                .baseUrl(wmInfo.getHttpBaseUrl())
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .build();
        adapter = new BlockchainAdapter(webClient);
    }

    @Test
    void shouldFetchBlockchainStatus() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/blockchain/payments/PAY-001"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "paymentId": "PAY-001",
                                  "txHash": "0xabc123",
                                  "chain": "base",
                                  "confirmations": 2,
                                  "amount": "1000.00",
                                  "currency": "USDC",
                                  "fromAddress": "0xSender",
                                  "toAddress": "0xReceiver",
                                  "blockTimestamp": "2026-03-08T10:31:00Z",
                                  "status": "CONFIRMED"
                                }
                                """)));

        // when
        var result = adapter.fetchBlockchainStatus("PAY-001");

        // then
        assertThat(result.paymentId()).isEqualTo("PAY-001");
        assertThat(result.txHash()).isEqualTo("0xabc123");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void shouldThrowPaymentNotFoundOn404() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/blockchain/payments/PAY-MISSING"))
                .willReturn(aResponse().withStatus(404)));

        // then
        assertThatThrownBy(() -> adapter.fetchBlockchainStatus("PAY-MISSING"))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void shouldThrowServiceUnavailableOn500() {
        // given
        stubFor(get(urlPathEqualTo("/api/v1/blockchain/payments/PAY-001"))
                .willReturn(aResponse().withStatus(500)));

        // then
        assertThatThrownBy(() -> adapter.fetchBlockchainStatus("PAY-001"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessageContaining("blockchain");
    }
}
