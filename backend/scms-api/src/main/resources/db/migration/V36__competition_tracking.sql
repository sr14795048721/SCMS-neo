-- Competition tracking: sources (official websites / wechat), auto-detected
-- leads awaiting admin confirmation, and the confirmed competition library.

CREATE TABLE IF NOT EXISTS competition_sources (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    url VARCHAR(500),
    source_type VARCHAR(32) NOT NULL DEFAULT 'OFFICIAL_WEBSITE',
    wechat_name VARCHAR(120),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    scan_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS competition_leads (
    id BIGSERIAL PRIMARY KEY,
    source_id BIGINT,
    title VARCHAR(300) NOT NULL,
    url VARCHAR(500),
    snippet TEXT,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS competitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(300) NOT NULL,
    category VARCHAR(120),
    participant_scope VARCHAR(200),
    apply_deadline TIMESTAMPTZ,
    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    url VARCHAR(500),
    source_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'UPCOMING',
    note TEXT,
    last_deadline_notified_at TIMESTAMPTZ,
    last_start_notified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_competition_leads_status ON competition_leads(status);
CREATE INDEX IF NOT EXISTS idx_competition_sources_enabled ON competition_sources(enabled);
