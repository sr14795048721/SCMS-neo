package com.scms.core.ai;

import com.scms.core.ai.client.SiliconFlowAiClient;
import com.scms.core.ai.client.SiliconFlowChatCompletionRequest;
import com.scms.core.ai.client.SiliconFlowChatCompletionResponse;
import com.scms.core.ai.config.AiProperties;
import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.ai.service.AiCommitService;
import com.scms.core.ai.service.ScmsAiCompletionService;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.security.AuthenticatedUser;
import com.scms.core.security.CurrentUserProvider;
import com.scms.core.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiCommitServiceTest {

    private SiliconFlowAiClient aiClient;
    private CurrentUserProvider currentUserProvider;
    private AiProperties properties;
    private ScmsAiCompletionService completionService;
    private AiCommitService service;

    @BeforeEach
    void setUp() {
        aiClient = mock(SiliconFlowAiClient.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        properties = new AiProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://api.siliconflow.cn");
        properties.setApiKey("secret-key");
        properties.setModel("Pro/deepseek-ai/DeepSeek-V3.1-Terminus");
        properties.setSystemPrompt("You are a helpful assistant");
        properties.setTimeoutSeconds(60);

        completionService = new ScmsAiCompletionService(aiClient, properties);
        service = new AiCommitService(completionService, currentUserProvider);
        when(currentUserProvider.getRequiredUser())
                .thenReturn(new AuthenticatedUser(7L, "student", UserRole.STUDENT));
    }

    @Test
    void completeCommitShouldReturnNormalizedModelResponse() {
        when(aiClient.createCompletion(any())).thenReturn(new SiliconFlowChatCompletionResponse(
                "Pro/deepseek-ai/DeepSeek-V3.1-Terminus",
                List.of(new SiliconFlowChatCompletionResponse.Choice(
                        new SiliconFlowChatCompletionResponse.Message("assistant", "你好，我是测试模型"),
                        "stop"
                )),
                new SiliconFlowChatCompletionResponse.Usage(12, 85, 97)
        ));

        AiCommitResponse response = service.completeCommit("  你好，请介绍一下你自己  ");

        ArgumentCaptor<SiliconFlowChatCompletionRequest> requestCaptor =
                ArgumentCaptor.forClass(SiliconFlowChatCompletionRequest.class);
        verify(aiClient).createCompletion(requestCaptor.capture());
        SiliconFlowChatCompletionRequest request = requestCaptor.getValue();

        assertEquals("Pro/deepseek-ai/DeepSeek-V3.1-Terminus", request.model());
        assertEquals(2, request.messages().size());
        assertEquals("system", request.messages().get(0).role());
        assertEquals("You are a helpful assistant", request.messages().get(0).content());
        assertEquals("user", request.messages().get(1).role());
        assertEquals("你好，请介绍一下你自己", request.messages().get(1).content());
        assertEquals("你好，我是测试模型", response.result());
        assertEquals("siliconflow", response.provider());
        assertEquals("stop", response.finishReason());
        assertEquals(97, response.usage().totalTokens());
    }

    @Test
    void completeCommitShouldRejectBlankCommit() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.completeCommit("   ")
        );

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(aiClient, never()).createCompletion(any());
    }

    @Test
    void completeCommitShouldFailWhenServiceDisabled() {
        properties.setEnabled(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.completeCommit("hello")
        );

        assertEquals(ErrorCode.SYSTEM_ERROR, exception.getErrorCode());
        verify(aiClient, never()).createCompletion(any());
    }

    @Test
    void completeCommitShouldFailWhenUpstreamResponseMissingChoice() {
        when(aiClient.createCompletion(any())).thenReturn(new SiliconFlowChatCompletionResponse(
                "Pro/deepseek-ai/DeepSeek-V3.1-Terminus",
                List.of(),
                new SiliconFlowChatCompletionResponse.Usage(0, 0, 0)
        ));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.completeCommit("hello")
        );

        assertEquals(ErrorCode.SYSTEM_ERROR, exception.getErrorCode());
    }

    @Test
    void completeCommitShouldWrapUpstreamHttpFailure() {
        when(aiClient.createCompletion(any())).thenThrow(new RestClientResponseException(
                "Service Unavailable", 503, "Service Unavailable", null, null, null));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.completeCommit("hello")
        );

        assertEquals(ErrorCode.SYSTEM_ERROR, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("ai service unavailable"));
    }
}
