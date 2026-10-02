package com.scms.core.ai;

import com.scms.core.ai.controller.AiCommitController;
import com.scms.core.ai.dto.AiCommitRequest;
import com.scms.core.ai.dto.AiCommitResponse;
import com.scms.core.ai.service.AiCommitService;
import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCommitControllerTest {

    @Test
    void commitShouldWrapResponseWithApiEnvelope() {
        AiCommitService service = mock(AiCommitService.class);
        AiCommitController controller = new AiCommitController(service, new ApiResponseFactory());
        AiCommitResponse payload = new AiCommitResponse(
                "hello",
                "siliconflow",
                "Pro/deepseek-ai/DeepSeek-V3.1-Terminus",
                "stop",
                new AiCommitResponse.Usage(10, 20, 30)
        );

        when(service.completeCommit("Introduce yourself")).thenReturn(payload);

        ApiResponse<AiCommitResponse> response = controller.commit(new AiCommitRequest("Introduce yourself"));

        assertEquals("OK", response.code());
        assertEquals("success", response.message());
        assertNotNull(response.requestId());
        assertEquals("hello", response.data().result());
        assertEquals("siliconflow", response.data().provider());
    }
}
