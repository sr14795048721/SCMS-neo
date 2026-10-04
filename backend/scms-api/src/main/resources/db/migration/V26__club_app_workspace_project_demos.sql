CREATE TABLE IF NOT EXISTS club_app_workspace_project_demo_profiles (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES club_app_workspace_projects(id) ON DELETE CASCADE,
    overview_title VARCHAR(180) NOT NULL,
    overview_body VARCHAR(4000) NOT NULL,
    bridge_mode VARCHAR(60) NOT NULL,
    mock_snapshot_json TEXT NOT NULL DEFAULT '{}',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_app_workspace_project_demo_profiles_project
    ON club_app_workspace_project_demo_profiles(project_id);

CREATE TABLE IF NOT EXISTS club_app_workspace_project_demo_steps (
    id BIGSERIAL PRIMARY KEY,
    demo_profile_id BIGINT NOT NULL REFERENCES club_app_workspace_project_demo_profiles(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(1000) NOT NULL DEFAULT '',
    trigger_type VARCHAR(40) NOT NULL,
    target_subsystem VARCHAR(60) NOT NULL,
    sort_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_project_demo_steps_profile_order
    ON club_app_workspace_project_demo_steps(demo_profile_id, sort_order, id);

CREATE TABLE IF NOT EXISTS club_app_workspace_project_demo_devices (
    id BIGSERIAL PRIMARY KEY,
    demo_profile_id BIGINT NOT NULL REFERENCES club_app_workspace_project_demo_profiles(id) ON DELETE CASCADE,
    device_key VARCHAR(120) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    role VARCHAR(80) NOT NULL,
    connection_type VARCHAR(60) NOT NULL,
    sort_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_project_demo_devices_profile_order
    ON club_app_workspace_project_demo_devices(demo_profile_id, sort_order, id);

CREATE TABLE IF NOT EXISTS club_app_workspace_project_demo_runtime_snapshots (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES club_app_workspace_projects(id) ON DELETE CASCADE,
    source_type VARCHAR(60) NOT NULL,
    source_session_id VARCHAR(120) NOT NULL,
    payload_json TEXT NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_app_workspace_project_demo_runtime_project
    ON club_app_workspace_project_demo_runtime_snapshots(project_id);

CREATE TABLE IF NOT EXISTS club_app_workspace_project_demo_events (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES club_app_workspace_projects(id) ON DELETE CASCADE,
    source_session_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    detail VARCHAR(2000) NOT NULL DEFAULT '',
    level VARCHAR(40) NOT NULL DEFAULT 'INFO',
    event_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_project_demo_events_project_time
    ON club_app_workspace_project_demo_events(project_id, event_at DESC, id DESC);
