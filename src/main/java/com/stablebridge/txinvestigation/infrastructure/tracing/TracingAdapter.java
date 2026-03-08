package com.stablebridge.txinvestigation.infrastructure.tracing;

import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.model.TraceSnapshot;
import com.stablebridge.txinvestigation.domain.model.TraceSpan;
import com.stablebridge.txinvestigation.domain.port.TraceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.services.tracing.enabled", havingValue = "true", matchIfMissing = true)
class TracingAdapter implements TraceProvider {

    private final WebClient tracingWebClient;

    @Override
    public TraceSnapshot fetchTrace(String paymentId) {
        log.info("Fetching distributed trace for {}", paymentId);

        JsonNode json;
        try {
            json = tracingWebClient.get()
                    .uri("/api/traces?service=payment-orchestrator&tags={tags}&limit=1",
                            "{\"paymentId\":\"" + paymentId + "\"}")
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        throw new ServiceUnavailableException("tracing",
                                "HTTP " + response.statusCode().value());
                    })
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("No traces found for payment {} — returning empty snapshot", paymentId);
            return new TraceSnapshot(paymentId, null, 0, 0L, List.of());
        }

        return toTraceSnapshot(paymentId, json);
    }

    private TraceSnapshot toTraceSnapshot(String paymentId, JsonNode json) {
        if (!json.has("data") || json.get("data").isEmpty()) {
            return new TraceSnapshot(paymentId, null, 0, 0L, List.of());
        }

        var trace = json.get("data").get(0);
        var traceId = trace.get("traceID").asText();
        long durationMs = trace.get("durationMs").asLong();

        List<TraceSpan> spans = new ArrayList<>();
        if (trace.has("spans")) {
            for (var spanNode : trace.get("spans")) {
                spans.add(new TraceSpan(
                        spanNode.get("spanID").asText(),
                        spanNode.has("parentSpanID") && !spanNode.get("parentSpanID").isNull()
                                ? spanNode.get("parentSpanID").asText() : null,
                        spanNode.get("operationName").asText(),
                        spanNode.get("serviceName").asText(),
                        Instant.parse(spanNode.get("startTime").asText()),
                        spanNode.get("durationMs").asLong(),
                        spanNode.get("status").asText(),
                        spanNode.has("errorMessage") && !spanNode.get("errorMessage").isNull()
                                ? spanNode.get("errorMessage").asText() : null));
            }
        }

        return new TraceSnapshot(paymentId, traceId, spans.size(), durationMs, spans);
    }
}
