package com.scms.core.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "silicon-flow-ai-client",
        url = "${scms.ai.base-url:https://api.siliconflow.cn}",
        configuration = SiliconFlowAiClientConfig.class
)
public interface SiliconFlowAiClient {

    @PostMapping(value = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    SiliconFlowChatCompletionResponse createCompletion(@RequestBody SiliconFlowChatCompletionRequest request);
}
