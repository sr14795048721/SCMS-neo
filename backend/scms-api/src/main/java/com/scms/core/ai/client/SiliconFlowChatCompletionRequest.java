package com.scms.core.ai.client;

import java.util.List;

public record SiliconFlowChatCompletionRequest(
        String model,
        List<Message> messages
) {
    public record Message(
            String role,
            String content
    ) {
    }
}
