package com.scms.core.yinzhi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import com.scms.core.yinzhi.controller.YinzhiDemoObserverController;
import com.scms.core.yinzhi.dto.YinzhiAiInspectionResponse;
import com.scms.core.yinzhi.service.YinzhiAiInspectionQueryService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YinzhiDemoObserverControllerTest {

    @Test
    void observerPageShouldRenderOnlineTrendChart() throws Exception {
        YinzhiAiInspectionQueryService queryService = mock(YinzhiAiInspectionQueryService.class);
        IotDeviceTelemetryEventRepository eventRepository = mock(IotDeviceTelemetryEventRepository.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        YinzhiDemoObserverController controller = new YinzhiDemoObserverController(
                queryService,
                eventRepository,
                objectMapper,
                "demo-token"
        );
        when(queryService.getObserverLatest()).thenReturn(new YinzhiAiInspectionResponse(
                Instant.parse("2026-05-14T00:00:00Z"),
                false,
                null,
                List.of(),
                List.of(),
                List.of()
        ));
        when(eventRepository.findAllByProjectAndReceivedAtAfterOrderByReceivedAtAscIdAsc(eq("yinzhi_guanjia"), any()))
                .thenReturn(List.of(sampleOnlineEvent()));

        String html = controller.yinzhiGuanjia("demo-token");

        assertTrue(html.contains("设备在线趋势（近 24h）"));
        assertTrue(html.contains("在线得分 = WiFi 10 分 + ATmega328 10 分 + 摄像头 10 分"));
        assertTrue(html.contains(".trend-svg{width:100%;"));
        assertTrue(html.contains("<polyline class=\"trend-line\""));
        assertTrue(html.contains("<circle class=\"trend-point\""));
    }

    private IotDeviceTelemetryEventEntity sampleOnlineEvent() throws Exception {
        IotDeviceTelemetryEventEntity event = new IotDeviceTelemetryEventEntity();
        event.setProject("yinzhi_guanjia");
        event.setWifiConnected(true);
        event.setPayloadJson("""
                {
                  "atmega": {"online": true},
                  "vision": {"online": true}
                }
                """);
        setReceivedAt(event, Instant.now().minusSeconds(600));
        return event;
    }

    private void setReceivedAt(IotDeviceTelemetryEventEntity event, Instant receivedAt) throws Exception {
        Field field = IotDeviceTelemetryEventEntity.class.getDeclaredField("receivedAt");
        field.setAccessible(true);
        field.set(event, receivedAt);
    }
}
