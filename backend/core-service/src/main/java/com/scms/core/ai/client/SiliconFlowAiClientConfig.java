package com.scms.core.ai.client;

import com.scms.core.ai.config.AiProperties;
import feign.Request;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

public class SiliconFlowAiClientConfig {

    @Bean
    public RequestInterceptor siliconFlowAiRequestInterceptor(AiProperties properties) {
        return template -> {
            String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
            if (!apiKey.isBlank()) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            }
            template.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        };
    }

    @Bean
    public Request.Options siliconFlowAiRequestOptions(AiProperties properties) {
        long timeout = Math.max(properties.getTimeoutSeconds(), 1);
        return new Request.Options(timeout, TimeUnit.SECONDS, timeout, TimeUnit.SECONDS, true);
    }
}
