package com.stablebridge.txinvestigation.infrastructure.compliance;

import com.stablebridge.txinvestigation.domain.model.ComplianceDecision;
import com.stablebridge.txinvestigation.domain.model.ComplianceSnapshot;
import com.stablebridge.txinvestigation.domain.model.PaymentNotFoundException;
import com.stablebridge.txinvestigation.domain.model.ScreeningResult;
import com.stablebridge.txinvestigation.domain.model.ServiceUnavailableException;
import com.stablebridge.txinvestigation.domain.port.ComplianceStateProvider;
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
@ConditionalOnProperty(name = "app.services.compliance.enabled", havingValue = "true", matchIfMissing = false)
class ComplianceAdapter implements ComplianceStateProvider {

    private final WebClient complianceWebClient;

    @Override
    public ComplianceSnapshot fetchComplianceStatus(String paymentId) {
        log.info("Fetching compliance status for {}", paymentId);

        var json = complianceWebClient.get()
                .uri("/api/v1/compliance/payments/{id}", paymentId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> {
                    throw new PaymentNotFoundException(paymentId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, response -> {
                    throw new ServiceUnavailableException("compliance",
                            "HTTP " + response.statusCode().value());
                })
                .bodyToMono(JsonNode.class)
                .block();

        return toComplianceSnapshot(json);
    }

    private ComplianceSnapshot toComplianceSnapshot(JsonNode json) {
        List<ComplianceDecision> decisions = new ArrayList<>();
        if (json.has("decisions")) {
            for (var node : json.get("decisions")) {
                decisions.add(new ComplianceDecision(
                        node.get("checkType").asText(),
                        node.get("result").asText(),
                        node.get("provider").asText(),
                        Instant.parse(node.get("timestamp").asText()),
                        node.get("detail").asText()));
            }
        }

        return new ComplianceSnapshot(
                json.get("paymentId").asText(),
                ScreeningResult.valueOf(json.get("screeningResult").asText()),
                json.get("travelRuleStatus").asText(),
                json.get("riskScore").asDouble(),
                decisions);
    }
}
