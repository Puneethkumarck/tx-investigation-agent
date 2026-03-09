package com.stablebridge.txinvestigation.infrastructure.elasticsearch;

import com.stablebridge.txinvestigation.domain.model.LogEntry;
import com.stablebridge.txinvestigation.domain.model.LogLevel;
import com.stablebridge.txinvestigation.domain.model.LogSnapshot;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.port.LogSearchProvider;
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
@ConditionalOnProperty(name = "app.services.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
class ElasticsearchAdapter implements LogSearchProvider {

    private final WebClient elasticsearchWebClient;

    @Override
    public LogSnapshot searchErrorLogs(String paymentId) {
        log.info("Searching error logs for {}", paymentId);

        var searchBody = """
                {
                  "query": {
                    "bool": {
                      "must": [
                        { "term": { "paymentId": "%s" } },
                        { "terms": { "level": ["ERROR", "WARN"] } }
                      ]
                    }
                  },
                  "sort": [{ "@timestamp": "desc" }],
                  "size": 50
                }
                """.formatted(paymentId);

        JsonNode json;
        try {
            json = elasticsearchWebClient.post()
                    .uri("/payment-logs-*/_search")
                    .header("Content-Type", "application/json")
                    .bodyValue(searchBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        throw new ServiceUnavailableException("elasticsearch",
                                "HTTP " + response.statusCode().value());
                    })
                    .bodyToMono(JsonNode.class)
                    .block();
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Elasticsearch index not found for payment logs — returning empty snapshot");
            return new LogSnapshot(paymentId, 0, List.of());
        }

        return toLogSnapshot(paymentId, json);
    }

    private LogSnapshot toLogSnapshot(String paymentId, JsonNode json) {
        var hits = json.get("hits");
        int totalHits = hits.get("total").get("value").asInt();

        List<LogEntry> entries = new ArrayList<>();
        for (var hit : hits.get("hits")) {
            var source = hit.get("_source");
            entries.add(new LogEntry(
                    Instant.parse(source.get("@timestamp").asText()),
                    LogLevel.valueOf(source.get("level").asText()),
                    source.get("service").asText(),
                    source.get("message").asText(),
                    source.has("traceId") ? source.get("traceId").asText() : null,
                    source.has("stackTrace") ? source.get("stackTrace").asText() : null));
        }

        return new LogSnapshot(paymentId, totalHits, entries);
    }
}
