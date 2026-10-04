package com.scms.core.ai.client;

import com.scms.core.ai.config.AiProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SiliconFlowAiClient {

    private final AiProperties properties;
    private final RestClient restClient;

    public SiliconFlowAiClient(AiProperties properties) {
        this.properties = properties;
        long timeoutSeconds = Math.max(properties.getTimeoutSeconds(), 1);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) (timeoutSeconds * 1000));
        factory.setReadTimeout((int) (timeoutSeconds * 1000));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public SiliconFlowChatCompletionResponse createCompletion(SiliconFlowChatCompletionRequest request) {
        String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        return restClient.post()
                .uri("/v1/chat/completions")
                .headers(headers -> {
                    if (!apiKey.isBlank()) {
                        headers.setBearerAuth(apiKey);
                    }
                })
                .body(request)
                .retrieve()
                .body(SiliconFlowChatCompletionResponse.class);
    }
}
