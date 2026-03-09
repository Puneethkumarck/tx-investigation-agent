package com.stablebridge.txinvestigation.infrastructure.orchestrator;

import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.PaymentState;
import com.stablebridge.txinvestigation.domain.model.PaymentStatus;
import com.stablebridge.txinvestigation.domain.model.SagaEvent;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.port.PaymentStateProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.services.orchestrator.enabled", havingValue = "true", matchIfMissing = false)
class OrchestratorAdapter implements PaymentStateProvider {

    private final WebClient orchestratorWebClient;

    @Override
    public PaymentState fetchPaymentState(String paymentId) {
        log.info("Fetching payment state for {}", paymentId);

        var json = orchestratorWebClient.get()
                .uri("/api/v1/payments/{id}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    throw new PaymentNotFoundException(paymentId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    throw new ServiceUnavailableException("orchestrator",
                            "HTTP " + response.statusCode().value());
                })
                .bodyToMono(JsonNode.class)
                .block();

        return toPaymentState(json);
    }

    private PaymentState toPaymentState(JsonNode json) {
        List<SagaEvent> events = new ArrayList<>();
        if (json.has("events")) {
            for (var eventNode : json.get("events")) {
                events.add(new SagaEvent(
                        eventNode.get("step").asText(),
                        eventNode.get("status").asText(),
                        Instant.parse(eventNode.get("timestamp").asText()),
                        eventNode.get("detail").asText()));
            }
        }

        return new PaymentState(
                json.get("paymentId").asText(),
                PaymentStatus.valueOf(json.get("status").asText()),
                json.get("sagaStep").asText(),
                json.get("workflowId").asText(),
                events,
                Instant.parse(json.get("createdAt").asText()),
                Instant.parse(json.get("updatedAt").asText()));
    }
}
