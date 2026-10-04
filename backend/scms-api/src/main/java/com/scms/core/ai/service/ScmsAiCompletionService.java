package com.scms.core.ai.service;

import com.scms.core.ai.client.SiliconFlowAiClient;
import com.scms.core.ai.client.SiliconFlowChatCompletionRequest;
import com.scms.core.ai.client.SiliconFlowChatCompletionResponse;
import com.scms.core.ai.config.AiProperties;
import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Service
public class ScmsAiCompletionService {

    private static final Logger log = LoggerFactory.getLogger(ScmsAiCompletionService.class);
    private static final String PROVIDER = "siliconflow";
    private static final String UNAVAILABLE_MESSAGE = "ai service unavailable";

    private final SiliconFlowAiClient aiClient;
    private final AiProperties properties;

    public ScmsAiCompletionService(SiliconFlowAiClient aiClient, AiProperties properties) {
        this.aiClient = aiClient;
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public AiCommitResponse complete(String userPrompt) {
        return complete(properties.getSystemPrompt(), userPrompt);
    }

    public AiCommitResponse complete(String systemPrompt, String userPrompt) {
        String normalizedPrompt = userPrompt == null ? "" : userPrompt.trim();
        if (normalizedPrompt.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "commit is required");
        }
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        }

        String normalizedSystemPrompt = systemPrompt == null || systemPrompt.isBlank()
                ? properties.getSystemPrompt().trim()
                : systemPrompt.trim();
        try {
            SiliconFlowChatCompletionResponse response = aiClient.createCompletion(new SiliconFlowChatCompletionRequest(
                    properties.getModel().trim(),
                    List.of(
                            new SiliconFlowChatCompletionRequest.Message("system", normalizedSystemPrompt),
                            new SiliconFlowChatCompletionRequest.Message("user", normalizedPrompt)
                    )
            ));
            return toResponse(response);
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("SCMS AI upstream failed provider={} model={} status={}", PROVIDER, properties.getModel(), exception.getStatusCode().value());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        } catch (RuntimeException exception) {
            log.error("SCMS AI request failed provider={} model={}", PROVIDER, properties.getModel(), exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        }
    }

    private AiCommitResponse toResponse(SiliconFlowChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        }

        SiliconFlowChatCompletionResponse.Choice choice = response.choices().get(0);
        String result = choice.message() == null || choice.message().content() == null
                ? ""
                : choice.message().content().trim();
        if (result.isBlank()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, UNAVAILABLE_MESSAGE);
        }

        SiliconFlowChatCompletionResponse.Usage usage = response.usage();
        return new AiCommitResponse(
                result,
                PROVIDER,
                nonBlankOrDefault(response.model(), properties.getModel()),
                nonBlankOrDefault(choice.finishReason(), ""),
                new AiCommitResponse.Usage(
                        usage == null || usage.promptTokens() == null ? 0 : usage.promptTokens(),
                        usage == null || usage.completionTokens() == null ? 0 : usage.completionTokens(),
                        usage == null || usage.totalTokens() == null ? 0 : usage.totalTokens()
                )
        );
    }

    private String nonBlankOrDefault(String value, String fallback) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? fallback : normalized;
    }
}
