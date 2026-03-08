package com.stablebridge.txinvestigation.infrastructure.ledger;

import com.stablebridge.txinvestigation.domain.model.Direction;
import com.stablebridge.txinvestigation.domain.model.LedgerEntry;
import com.stablebridge.txinvestigation.domain.model.LedgerSnapshot;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.port.LedgerStateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.services.ledger.enabled", havingValue = "true", matchIfMissing = true)
class LedgerAdapter implements LedgerStateProvider {

    private final WebClient ledgerWebClient;

    @Override
    public LedgerSnapshot fetchLedgerEntries(String paymentId) {
        log.info("Fetching ledger entries for {}", paymentId);

        var json = ledgerWebClient.get()
                .uri("/api/v1/ledger/payments/{id}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    throw new PaymentNotFoundException(paymentId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    throw new ServiceUnavailableException("ledger",
                            "HTTP " + response.statusCode().value());
                })
                .bodyToMono(JsonNode.class)
                .block();

        return toLedgerSnapshot(json);
    }

    private LedgerSnapshot toLedgerSnapshot(JsonNode json) {
        List<LedgerEntry> entries = new ArrayList<>();
        if (json.has("entries")) {
            for (var node : json.get("entries")) {
                entries.add(new LedgerEntry(
                        node.get("entryId").asText(),
                        node.get("account").asText(),
                        Direction.valueOf(node.get("direction").asText()),
                        new BigDecimal(node.get("amount").asText()),
                        node.get("currency").asText(),
                        Instant.parse(node.get("timestamp").asText())));
            }
        }

        return new LedgerSnapshot(
                json.get("paymentId").asText(),
                entries,
                new BigDecimal(json.get("netPosition").asText()),
                json.get("settlementStatus").asText());
    }
}
