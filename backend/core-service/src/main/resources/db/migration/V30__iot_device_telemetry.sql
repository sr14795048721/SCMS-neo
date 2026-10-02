CREATE TABLE IF NOT EXISTS iot_device_telemetry_latest_states (
    id BIGSERIAL PRIMARY KEY,
    schema_name VARCHAR(64) NOT NULL,
    project VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL UNIQUE,
    gateway_type VARCHAR(64) NOT NULL,
    firmware VARCHAR(128),
    mac VARCHAR(32),
    event_name VARCHAR(64) NOT NULL,
    upload_sequence BIGINT NOT NULL,
    sent_at_ms BIGINT NOT NULL,
    gateway_uptime_ms BIGINT,
    pending_reason VARCHAR(64),
    wifi_connected BOOLEAN NOT NULL DEFAULT FALSE,
    ip VARCHAR(64),
    rssi INTEGER,
    user_agent VARCHAR(255),
    payload_json TEXT NOT NULL,
    first_received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_received_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_iot_device_telemetry_latest_states_project
    ON iot_device_telemetry_latest_states(project);

CREATE INDEX IF NOT EXISTS idx_iot_device_telemetry_latest_states_last_received_at
    ON iot_device_telemetry_latest_states(last_received_at DESC);

CREATE TABLE IF NOT EXISTS iot_device_telemetry_events (
    id BIGSERIAL PRIMARY KEY,
    schema_name VARCHAR(64) NOT NULL,
    project VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    gateway_type VARCHAR(64) NOT NULL,
    firmware VARCHAR(128),
    mac VARCHAR(32),
    event_name VARCHAR(64) NOT NULL,
    upload_sequence BIGINT NOT NULL,
    sent_at_ms BIGINT NOT NULL,
    gateway_uptime_ms BIGINT,
    pending_reason VARCHAR(64),
    wifi_connected BOOLEAN NOT NULL DEFAULT FALSE,
    ip VARCHAR(64),
    rssi INTEGER,
    user_agent VARCHAR(255),
    payload_json TEXT NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_iot_device_telemetry_events_device_sequence UNIQUE (device_id, upload_sequence)
);

CREATE INDEX IF NOT EXISTS idx_iot_device_telemetry_events_device_id
    ON iot_device_telemetry_events(device_id);

CREATE INDEX IF NOT EXISTS idx_iot_device_telemetry_events_received_at
    ON iot_device_telemetry_events(received_at DESC);
