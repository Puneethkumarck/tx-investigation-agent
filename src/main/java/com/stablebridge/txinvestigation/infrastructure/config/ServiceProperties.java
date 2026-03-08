package com.stablebridge.txinvestigation.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.services")
public record ServiceProperties(
        ServiceEndpoint orchestrator,
        ServiceEndpoint compliance,
        ServiceEndpoint blockchain,
        ServiceEndpoint ledger
) {

    public record ServiceEndpoint(String baseUrl) {}
}
