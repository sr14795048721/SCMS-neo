ALTER TABLE iot_device_telemetry_events
    DROP CONSTRAINT IF EXISTS uq_iot_device_telemetry_events_device_sequence;

CREATE INDEX IF NOT EXISTS idx_iot_device_telemetry_events_device_sequence_received
    ON iot_device_telemetry_events(device_id, upload_sequence, received_at DESC, id DESC);

ALTER TABLE yinzhi_ai_inspection_runs
    DROP CONSTRAINT IF EXISTS uq_yinzhi_ai_inspection_runs_device_sequence;

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_inspection_runs_device_sequence_created
    ON yinzhi_ai_inspection_runs(device_id, upload_sequence, created_at DESC, id DESC);
