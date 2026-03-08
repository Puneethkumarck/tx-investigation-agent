package com.stablebridge.txinvestigation.infrastructure.blockchain;

import com.stablebridge.txinvestigation.domain.model.BlockchainSnapshot;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.port.BlockchainStateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.services.blockchain.enabled", havingValue = "true", matchIfMissing = true)
class BlockchainAdapter implements BlockchainStateProvider {

    private final WebClient blockchainWebClient;

    @Override
    public BlockchainSnapshot fetchBlockchainStatus(String paymentId) {
        log.info("Fetching blockchain status for {}", paymentId);

        var json = blockchainWebClient.get()
                .uri("/api/v1/blockchain/payments/{id}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    throw new PaymentNotFoundException(paymentId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    throw new ServiceUnavailableException("blockchain",
                            "HTTP " + response.statusCode().value());
                })
                .bodyToMono(JsonNode.class)
                .block();

        return toBlockchainSnapshot(json);
    }

    private BlockchainSnapshot toBlockchainSnapshot(JsonNode json) {
        return new BlockchainSnapshot(
                json.get("paymentId").asText(),
                json.get("txHash").asText(),
                json.get("chain").asText(),
                json.get("confirmations").asInt(),
                new BigDecimal(json.get("amount").asText()),
                json.get("currency").asText(),
                json.get("fromAddress").asText(),
                json.get("toAddress").asText(),
                Instant.parse(json.get("blockTimestamp").asText()),
                json.get("status").asText());
    }
}
