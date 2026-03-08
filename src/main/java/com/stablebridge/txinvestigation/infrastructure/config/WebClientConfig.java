package com.stablebridge.txinvestigation.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JdkClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties(ServiceProperties.class)
class WebClientConfig {

    @Bean
    WebClient orchestratorWebClient(ServiceProperties properties) {
        return buildWebClient(properties.orchestrator().baseUrl());
    }

    @Bean
    WebClient complianceWebClient(ServiceProperties properties) {
        return buildWebClient(properties.compliance().baseUrl());
    }

    @Bean
    WebClient blockchainWebClient(ServiceProperties properties) {
        return buildWebClient(properties.blockchain().baseUrl());
    }

    @Bean
    WebClient ledgerWebClient(ServiceProperties properties) {
        return buildWebClient(properties.ledger().baseUrl());
    }

    private WebClient buildWebClient(String baseUrl) {
        var httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new JdkClientHttpConnector(httpClient))
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
