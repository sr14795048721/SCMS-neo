package com.scms.core.ai.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SiliconFlowChatCompletionResponse(
        String model,
        List<Choice> choices,
        Usage usage
) {
    public record Choice(
            Message message,
            @JsonProperty("finish_reason")
            String finishReason
    ) {
    }

    public record Message(
            String role,
            String content
    ) {
    }

    public record Usage(
            @JsonProperty("prompt_tokens")
            Integer promptTokens,
            @JsonProperty("completion_tokens")
            Integer completionTokens,
            @JsonProperty("total_tokens")
            Integer totalTokens
    ) {
    }
}
