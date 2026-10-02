package com.scms.core.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scms.core.common.error.ErrorCode;
import com.scms.core.common.exception.BusinessException;
import com.scms.core.iot.domain.IotDeviceTelemetryEventEntity;
import com.scms.core.iot.domain.IotDeviceTelemetryLatestStateEntity;
import com.scms.core.iot.dto.CyclingGuardianTelemetryRequest;
import com.scms.core.iot.dto.Esp32TelemetryRequest;
import com.scms.core.iot.dto.IotDeviceTelemetryRequest;
import com.scms.core.iot.repository.IotDeviceTelemetryEventRepository;
import com.scms.core.iot.repository.IotDeviceTelemetryLatestStateRepository;
import com.scms.core.iot.service.IotDeviceTelemetryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IotDeviceTelemetryServiceTest {

    private IotDeviceTelemetryLatestStateRepository latestStateRepository;
    private IotDeviceTelemetryEventRepository eventRepository;
    private IotDeviceTelemetryService service;

    @BeforeEach
    void setUp() {
        latestStateRepository = mock(IotDeviceTelemetryLatestStateRepository.class);
        eventRepository = mock(IotDeviceTelemetryEventRepository.class);
        service = new IotDeviceTelemetryService(latestStateRepository, eventRepository, new ObjectMapper().findAndRegisterModules());
        when(eventRepository.findFirstByDeviceIdAndUploadSequenceAndPayloadJsonAndReceivedAtAfterOrderByReceivedAtDescIdDesc(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(Optional.empty());
    }

    @Test
    void ingestShouldPersistLatestStateAndHistoryForNewDevice() {
        IotDeviceTelemetryRequest request = sampleRequest(6L, "vision_result");

        when(latestStateRepository.findByDeviceId("esp32s3_28372f89824c")).thenReturn(Optional.empty());

        service.ingest(request, "yinzhi-guanjia-esp32s3/1.0");

        verify(latestStateRepository).save(any());
        verify(eventRepository).save(any());
    }

    @Test
    void ingestShouldSkipDuplicateHistoryButKeepLatestSnapshotFresh() {
        IotDeviceTelemetryRequest request = sampleRequest(6L, "heartbeat");
        IotDeviceTelemetryLatestStateEntity existing = new IotDeviceTelemetryLatestStateEntity();
        existing.setDeviceId("esp32s3_28372f89824c");
        existing.setUploadSequence(6L);

        when(latestStateRepository.findByDeviceId("esp32s3_28372f89824c")).thenReturn(Optional.of(existing));
        when(eventRepository.findFirstByDeviceIdAndUploadSequenceAndPayloadJsonAndReceivedAtAfterOrderByReceivedAtDescIdDesc(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(Optional.of(new IotDeviceTelemetryEventEntity()));

        service.ingest(request, "yinzhi-guanjia-esp32s3/1.0");

        verify(latestStateRepository).save(any());
        verify(eventRepository, never()).save(any());
    }

    @Test
    void ingestShouldIgnoreOlderSequenceForLatestSnapshot() {
        IotDeviceTelemetryRequest request = sampleRequest(4L, "heartbeat");
        IotDeviceTelemetryLatestStateEntity existing = new IotDeviceTelemetryLatestStateEntity();
        existing.setDeviceId("esp32s3_28372f89824c");
        existing.setUploadSequence(7L);

        when(latestStateRepository.findByDeviceId("esp32s3_28372f89824c")).thenReturn(Optional.of(existing));

        service.ingest(request, "yinzhi-guanjia-esp32s3/1.0");

        verify(latestStateRepository, never()).save(any());
        verify(eventRepository).save(any());
    }

    @Test
    void ingestShouldPersistHistoryAfterGatewayRestartResetsSequence() {
        IotDeviceTelemetryRequest request = sampleRequest(1L, "wifi_connected");
        IotDeviceTelemetryLatestStateEntity existing = new IotDeviceTelemetryLatestStateEntity();
        existing.setDeviceId("esp32s3_28372f89824c");
        existing.setUploadSequence(18L);
        existing.setGatewayUptimeMs(320_000L);

        when(latestStateRepository.findByDeviceId("esp32s3_28372f89824c")).thenReturn(Optional.of(existing));

        service.ingest(request, "yinzhi-guanjia-esp32s3/1.0");

        verify(latestStateRepository).save(any());
        verify(eventRepository).save(any());
    }

    @Test
    void ingestShouldRejectUnexpectedProject() {
        IotDeviceTelemetryRequest request = new IotDeviceTelemetryRequest(
                IotDeviceTelemetryService.EXPECTED_SCHEMA,
                "other_project",
                "heartbeat",
                1000L,
                sampleDevice(),
                sampleNetwork(),
                sampleGateway(1L),
                sampleAtmega(),
                sampleVision(),
                sampleDiagnostics()
        );

        BusinessException exception = assertThrows(BusinessException.class, () -> service.ingest(request, null));

        assertEquals(ErrorCode.VALIDATION_ERROR, exception.getErrorCode());
        verify(latestStateRepository, never()).findByDeviceId(any());
        verify(eventRepository, never())
                .findFirstByDeviceIdAndUploadSequenceAndPayloadJsonAndReceivedAtAfterOrderByReceivedAtDescIdDesc(
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void ingestEsp32TelemetryShouldPersistLatestStateAndHistory() throws Exception {
        Esp32TelemetryRequest request = sampleEsp32Request("telemetry", "uart_to_wifi");
        long uploadSequence = Instant.parse("2026-04-24T21:35:12Z").toEpochMilli();

        when(latestStateRepository.findByDeviceId("gengyu-zhice-esp32-01")).thenReturn(Optional.empty());

        service.ingest(request, "gengyu-zhice-esp32/1.0");

        ArgumentCaptor<IotDeviceTelemetryLatestStateEntity> latestCaptor = forClass(IotDeviceTelemetryLatestStateEntity.class);
        ArgumentCaptor<IotDeviceTelemetryEventEntity> eventCaptor = forClass(IotDeviceTelemetryEventEntity.class);
        verify(latestStateRepository).save(latestCaptor.capture());
        verify(eventRepository).save(eventCaptor.capture());

        IotDeviceTelemetryLatestStateEntity latestState = latestCaptor.getValue();
        assertEquals(IotDeviceTelemetryService.ESP32_TELEMETRY_SCHEMA, latestState.getSchemaName());
        assertEquals(IotDeviceTelemetryService.ESP32_TELEMETRY_PROJECT, latestState.getProject());
        assertEquals("gengyu-zhice-esp32-01", latestState.getDeviceId());
        assertEquals("uart_to_wifi", latestState.getGatewayType());
        assertEquals("telemetry", latestState.getEventName());
        assertEquals(uploadSequence, latestState.getUploadSequence());
        assertEquals(uploadSequence, latestState.getSentAtMs());
        assertEquals(18423L, latestState.getGatewayUptimeMs());
        assertEquals("gengyu-zhice-mega-01", latestState.getPendingReason());
        assertEquals(-56, latestState.getRssi());
        var savedPayload = new ObjectMapper().readTree(latestState.getPayloadJson());
        assertTrue(savedPayload.has("payload"));
        assertTrue(savedPayload.has("edge_inference"));
        assertEquals("HIGH", savedPayload.get("edge_inference").get("overall_risk").asText());
        assertEquals(uploadSequence, eventCaptor.getValue().getUploadSequence());
    }

    @Test
    void esp32TelemetryRequestShouldRejectUnexpectedFixedFieldsAndEmptyPayload() throws Exception {
        assertFalse(sampleEsp32Request("heartbeat", "uart_to_wifi").isMessageTypeValid());
        assertFalse(sampleEsp32Request("telemetry", "wifi").isTransportValid());
        Esp32TelemetryRequest emptyPayload = new Esp32TelemetryRequest(
                "gengyu-zhice-esp32-01",
                "gengyu-zhice-mega-01",
                "telemetry",
                "uart_to_wifi",
                Instant.parse("2026-04-24T21:35:12Z"),
                18423L,
                -56,
                null,
                new ObjectMapper().readTree("{}")
        );
        assertFalse(emptyPayload.isPayloadObject());
    }

    @Test
    void ingestCyclingGuardianTelemetryShouldPersistLatestStateAndHistory() throws Exception {
        CyclingGuardianTelemetryRequest request = sampleCyclingGuardianRequest("telemetry", "jetson_http");
        long sentAtMs = Instant.parse("2026-04-24T21:35:12Z").toEpochMilli();

        when(latestStateRepository.findByDeviceId("cycling-guardian-jetson-01")).thenReturn(Optional.empty());

        service.ingest(request, "cycling-guardian-jetson/1.0");

        ArgumentCaptor<IotDeviceTelemetryLatestStateEntity> latestCaptor = forClass(IotDeviceTelemetryLatestStateEntity.class);
        ArgumentCaptor<IotDeviceTelemetryEventEntity> eventCaptor = forClass(IotDeviceTelemetryEventEntity.class);
        verify(latestStateRepository).save(latestCaptor.capture());
        verify(eventRepository).save(eventCaptor.capture());

        IotDeviceTelemetryLatestStateEntity latestState = latestCaptor.getValue();
        assertEquals(IotDeviceTelemetryService.CYCLING_GUARDIAN_TELEMETRY_SCHEMA, latestState.getSchemaName());
        assertEquals(IotDeviceTelemetryService.CYCLING_GUARDIAN_TELEMETRY_PROJECT, latestState.getProject());
        assertEquals("cycling-guardian-jetson-01", latestState.getDeviceId());
        assertEquals("jetson_http", latestState.getGatewayType());
        assertEquals("telemetry", latestState.getEventName());
        assertEquals(12L, latestState.getUploadSequence());
        assertEquals(sentAtMs, latestState.getSentAtMs());
        assertEquals(184230L, latestState.getGatewayUptimeMs());
        assertEquals("jetson-nano-b01", latestState.getPendingReason());
        assertTrue(new ObjectMapper().readTree(latestState.getPayloadJson()).has("payload"));
        assertEquals(12L, eventCaptor.getValue().getUploadSequence());
    }

    @Test
    void cyclingGuardianTelemetryRequestShouldRejectUnexpectedFixedFieldsAndEmptyPayload() throws Exception {
        assertFalse(sampleCyclingGuardianRequest("heartbeat", "jetson_http").isMessageTypeValid());
        assertFalse(sampleCyclingGuardianRequest("telemetry", "uart_to_wifi").isTransportValid());
        CyclingGuardianTelemetryRequest emptyPayload = new CyclingGuardianTelemetryRequest(
                "cycling-guardian-jetson-01",
                "jetson-nano-b01",
                "telemetry",
                "jetson_http",
                Instant.parse("2026-04-24T21:35:12Z"),
                184230L,
                12L,
                new ObjectMapper().readTree("{}")
        );
        assertFalse(emptyPayload.isPayloadObject());
    }

    private IotDeviceTelemetryRequest sampleRequest(Long uploadSequence, String event) {
        return new IotDeviceTelemetryRequest(
                IotDeviceTelemetryService.EXPECTED_SCHEMA,
                IotDeviceTelemetryService.EXPECTED_PROJECT,
                event,
                84512L,
                sampleDevice(),
                sampleNetwork(),
                sampleGateway(uploadSequence),
                sampleAtmega(),
                sampleVision(),
                sampleDiagnostics()
        );
    }

    private IotDeviceTelemetryRequest.Device sampleDevice() {
        return new IotDeviceTelemetryRequest.Device(
                "esp32s3_28372f89824c",
                "esp32s3_gateway",
                "esp32_state_receiver_net_v1",
                "28:37:2f:89:82:4c"
        );
    }

    private IotDeviceTelemetryRequest.Network sampleNetwork() {
        return new IotDeviceTelemetryRequest.Network("123", true, "192.168.1.88", -48);
    }

    private IotDeviceTelemetryRequest.Gateway sampleGateway(Long uploadSequence) {
        return new IotDeviceTelemetryRequest.Gateway(84512L, uploadSequence, "vision_result");
    }

    private IotDeviceTelemetryRequest.Atmega sampleAtmega() {
        return new IotDeviceTelemetryRequest.Atmega(
                true,
                true,
                84211L,
                17L,
                82300L,
                false,
                true,
                false,
                true,
                true,
                "garage_active",
                "garage_open"
        );
    }

    private IotDeviceTelemetryRequest.Vision sampleVision() {
        return new IotDeviceTelemetryRequest.Vision(
                true,
                true,
                true,
                true,
                84490L,
                9L,
                "block",
                1,
                149,
                118,
                58,
                73,
                0,
                0,
                0,
                0
        );
    }

    private IotDeviceTelemetryRequest.Diagnostics sampleDiagnostics() {
        return new IotDeviceTelemetryRequest.Diagnostics("", 200, "atmega_state_update");
    }

    private Esp32TelemetryRequest sampleEsp32Request(String messageType, String transport) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return new Esp32TelemetryRequest(
                "gengyu-zhice-esp32-01",
                "gengyu-zhice-mega-01",
                messageType,
                transport,
                Instant.parse("2026-04-24T21:35:12Z"),
                18423L,
                -56,
                objectMapper.readTree("""
                        {
                          "model": "distilled_soil_model_v1",
                          "runtime": "esp32_local",
                          "status": "ok",
                          "initial_conclusion": "risk=HIGH;action=WATER_RETENTION;crop=ACID_DRY_TOLERANT",
                          "overall_risk": "HIGH",
                          "action_code": "WATER_RETENTION",
                          "crop_group": "ACID_DRY_TOLERANT",
                          "moisture_class": "SEVERE_DRY",
                          "ph_class": "SUITABLE",
                          "temp_class": "SUITABLE",
                          "display": {
                            "risk": "HIGH",
                            "action": "WATER",
                            "crop": "ACID_DRY",
                            "moisture": "VDRY",
                            "ph": "OK",
                            "temp": "OK"
                          }
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "uptime_ms": 12000,
                          "soil_adc": 0,
                          "soil_voltage": 0.0,
                          "soil_percent": 0,
                          "soil_state": "DRY",
                          "ph_raw": 293,
                          "ph_voltage": 1.433,
                          "ph_value": 6.91,
                          "room_temp_c": 25.1,
                          "room_rh": 50.4,
                          "probe_temp_c": 20.5
                        }
                        """)
        );
    }

    private CyclingGuardianTelemetryRequest sampleCyclingGuardianRequest(String messageType, String transport) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        return new CyclingGuardianTelemetryRequest(
                "cycling-guardian-jetson-01",
                "jetson-nano-b01",
                messageType,
                transport,
                Instant.parse("2026-04-24T21:35:12Z"),
                184230L,
                12L,
                objectMapper.readTree("""
                        {
                          "scene": "road-scan",
                          "risk": {
                            "level": "WARNING",
                            "distance_alert": "front 0.54 m, brake now",
                            "posture": "tilted",
                            "gps": "GPS unavailable"
                          },
                          "camera": {
                            "ok": true,
                            "camera_status": "online",
                            "target_count": 1,
                            "risk_level": "MEDIUM",
                            "vehicle_num": {"car": 1, "truck": 0, "bus": 0, "motorbike": 0, "tricycle": 0, "carplate": 0},
                            "vehicle_info": []
                          },
                          "distance": {"ok": true, "distance_mm": 538, "distance_m": 0.538, "status": "ok"},
                          "motion": {"ok": true, "pitch_deg": 49.86, "roll_deg": -113.19, "posture": "tilted", "temperature_c": 22.22, "calibrated": false},
                          "gps": {"ok": false, "latitude": null, "longitude": null, "status": "no_fix"},
                          "led_command": {"mode": "segmented_warning", "transport": "arduino_serial", "serial_status": "ok", "serial_port": "/dev/ttyACM0", "protocol": "cgled_serial_v1", "last_line": "CGLED,STATE,..."},
                          "config": {"distance_warning_mm": 1000, "distance_notice_mm": 2500, "tilt_warning_deg": 35.0, "tilt_notice_deg": 18.0}
                        }
                        """)
        );
    }
}
