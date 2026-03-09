package com.stablebridge.txinvestigation.infrastructure.temporal;

import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.model.WorkflowEvent;
import com.stablebridge.txinvestigation.domain.model.WorkflowSnapshot;
import com.stablebridge.txinvestigation.domain.port.WorkflowHistoryProvider;
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
@ConditionalOnProperty(name = "app.services.temporal.enabled", havingValue = "true", matchIfMissing = false)
class TemporalAdapter implements WorkflowHistoryProvider {

    private final WebClient temporalWebClient;

    @Override
    public WorkflowSnapshot fetchWorkflowHistory(String paymentId) {
        log.info("Fetching workflow history for {}", paymentId);

        var json = temporalWebClient.get()
                .uri("/api/v1/workflows/{id}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    throw new PaymentNotFoundException(paymentId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    throw new ServiceUnavailableException("temporal",
                            "HTTP " + response.statusCode().value());
                })
                .bodyToMono(JsonNode.class)
                .block();

        return toWorkflowSnapshot(json);
    }

    private WorkflowSnapshot toWorkflowSnapshot(JsonNode json) {
        List<WorkflowEvent> events = new ArrayList<>();
        if (json.has("events")) {
            for (var eventNode : json.get("events")) {
                events.add(new WorkflowEvent(
                        eventNode.get("eventId").asLong(),
                        eventNode.get("eventType").asText(),
                        Instant.parse(eventNode.get("timestamp").asText()),
                        eventNode.has("activityType") ? eventNode.get("activityType").asText() : null,
                        eventNode.get("detail").asText()));
            }
        }

        return new WorkflowSnapshot(
                json.get("paymentId").asText(),
                json.get("workflowId").asText(),
                json.get("workflowType").asText(),
                json.get("status").asText(),
                Instant.parse(json.get("startTime").asText()),
                json.has("closeTime") && !json.get("closeTime").isNull()
                        ? Instant.parse(json.get("closeTime").asText()) : null,
                json.get("attemptCount").asInt(),
                json.get("taskQueue").asText(),
                events);
    }
}
