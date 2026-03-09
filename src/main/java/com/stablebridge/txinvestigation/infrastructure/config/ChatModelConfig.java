package com.stablebridge.txinvestigation.infrastructure.config;

import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ChatModelConfig {

    @Bean
    @ConditionalOnMissingBean
    public OllamaChatModel ollamaChatModel() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(300_000);
        var restClientBuilder = RestClient.builder()
                .requestFactory(requestFactory);
        var api = OllamaApi.builder()
                .baseUrl("http://localhost:11434")
                .restClientBuilder(restClientBuilder)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaChatOptions.builder()
                        .model("llama3.2:latest")
                        .build())
                .build();
    }
}
