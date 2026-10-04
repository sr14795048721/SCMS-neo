package com.scms.core.ai.dto;

public record AiCommitResponse(
        String result,
        String provider,
        String model,
        String finishReason,
        Usage usage
) {
    public record Usage(
            int promptTokens,
            int completionTokens,
            int totalTokens
    ) {
    }
}
