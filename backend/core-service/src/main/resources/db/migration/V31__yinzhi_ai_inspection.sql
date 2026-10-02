CREATE TABLE IF NOT EXISTS yinzhi_ai_inspection_runs (
    id BIGSERIAL PRIMARY KEY,
    telemetry_event_id BIGINT NOT NULL UNIQUE REFERENCES iot_device_telemetry_events(id) ON DELETE CASCADE,
    device_id VARCHAR(128) NOT NULL,
    upload_sequence BIGINT NOT NULL,
    trigger_reason VARCHAR(80) NOT NULL,
    owner_presence VARCHAR(32) NOT NULL,
    scene_label VARCHAR(120) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    permission_decision VARCHAR(80) NOT NULL,
    action_summary VARCHAR(500) NOT NULL,
    ai_provider VARCHAR(80),
    ai_model VARCHAR(160),
    ai_finish_reason VARCHAR(80),
    ai_prompt_tokens INTEGER NOT NULL DEFAULT 0,
    ai_completion_tokens INTEGER NOT NULL DEFAULT 0,
    ai_total_tokens INTEGER NOT NULL DEFAULT 0,
    ai_report TEXT NOT NULL,
    fallback_report BOOLEAN NOT NULL DEFAULT FALSE,
    raw_telemetry_json TEXT NOT NULL,
    decision_json TEXT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_yinzhi_ai_inspection_runs_device_sequence UNIQUE (device_id, upload_sequence)
);

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_inspection_runs_created_at
    ON yinzhi_ai_inspection_runs(created_at DESC);

CREATE TABLE IF NOT EXISTS yinzhi_ai_inspection_logs (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES yinzhi_ai_inspection_runs(id) ON DELETE CASCADE,
    telemetry_event_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    upload_sequence BIGINT NOT NULL,
    stage VARCHAR(80) NOT NULL,
    level VARCHAR(40) NOT NULL,
    message VARCHAR(1200) NOT NULL,
    raw_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_inspection_logs_run
    ON yinzhi_ai_inspection_logs(run_id, created_at ASC, id ASC);

CREATE TABLE IF NOT EXISTS yinzhi_ai_device_commands (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES yinzhi_ai_inspection_runs(id) ON DELETE CASCADE,
    telemetry_event_id BIGINT NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    upload_sequence BIGINT NOT NULL,
    command_type VARCHAR(80) NOT NULL,
    risk_level VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    detail VARCHAR(1200) NOT NULL,
    queued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered_at TIMESTAMPTZ,
    acked_at TIMESTAMPTZ,
    ack_message VARCHAR(1200)
);

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_device_commands_device_status
    ON yinzhi_ai_device_commands(device_id, status, queued_at ASC);

UPDATE club_app_workspace_project_demo_steps AS step
SET action_key = 'yinzhi-guanjia-ai-inspection'
FROM club_app_workspace_project_demo_profiles AS profile
         JOIN club_app_workspace_projects AS project
              ON project.id = profile.project_id
WHERE step.demo_profile_id = profile.id
  AND project.project_key = 'yinzhi-guanjia'
  AND (step.title LIKE '%巡检%' OR step.title LIKE '%自主%' OR step.sort_order = 2)
  AND (step.action_key IS NULL OR BTRIM(step.action_key) = '');
