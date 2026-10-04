ALTER TABLE yinzhi_ai_device_commands
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verification_telemetry_event_id BIGINT,
    ADD COLUMN IF NOT EXISTS verification_upload_sequence BIGINT,
    ADD COLUMN IF NOT EXISTS verification_message VARCHAR(1200);

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_device_commands_verification
    ON yinzhi_ai_device_commands(device_id, status, upload_sequence ASC, id ASC);
