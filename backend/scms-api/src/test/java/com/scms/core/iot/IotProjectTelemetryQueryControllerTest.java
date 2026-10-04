package com.scms.core.iot;

import com.scms.core.common.api.ApiResponse;
import com.scms.core.common.web.ApiResponseFactory;
import com.scms.core.iot.controller.IotProjectTelemetryQueryController;
import com.scms.core.iot.dto.IotProjectTelemetryResponse;
import com.scms.core.iot.service.IotProjectTelemetryQueryService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IotProjectTelemetryQueryControllerTest {

    @Test
    void detailShouldWrapResponseWithApiEnvelope() {
        IotProjectTelemetryQueryService queryService = mock(IotProjectTelemetryQueryService.class);
        IotProjectTelemetryQueryController controller =
                new IotProjectTelemetryQueryController(queryService, new ApiResponseFactory());

        IotProjectTelemetryResponse payload = new IotProjectTelemetryResponse(
                "yinzhi-guanjia",
                "yinzhi_guanjia",
                "ESP32_GATEWAY",
                true,
                List.of(),
                List.of(),
                1,
                30,
                88,
                true
        );
        when(queryService.getVisibleProjectTelemetry(9L, "yinzhi-guanjia", 1, 30)).thenReturn(payload);

        ApiResponse<IotProjectTelemetryResponse> response = controller.detail(9L, "yinzhi-guanjia", 1, 30);

        assertEquals("OK", response.code());
        assertEquals("success", response.message());
        assertNotNull(response.requestId());
        assertEquals("yinzhi-guanjia", response.data().projectKey());
        assertEquals(1, response.data().eventPage());
        assertEquals(30, response.data().eventSize());
        assertEquals(88, response.data().eventTotal());
        assertEquals(true, response.data().hasMoreEvents());
    }
}
