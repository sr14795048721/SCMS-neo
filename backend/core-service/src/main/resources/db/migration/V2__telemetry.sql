CREATE TABLE IF NOT EXISTS visit_events (
    id BIGSERIAL PRIMARY KEY,
    visitor_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT REFERENCES users(id),
    path VARCHAR(255) NOT NULL,
    visited_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_visit_events_visited_at ON visit_events(visited_at);
CREATE INDEX IF NOT EXISTS idx_visit_events_visitor_id ON visit_events(visitor_id);
CREATE INDEX IF NOT EXISTS idx_visit_events_session_id ON visit_events(session_id);

CREATE TABLE IF NOT EXISTS visitor_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    visitor_id VARCHAR(64) NOT NULL,
    user_id BIGINT REFERENCES users(id),
    last_path VARCHAR(255) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_agent VARCHAR(512)
);

CREATE INDEX IF NOT EXISTS idx_visitor_sessions_last_seen_at ON visitor_sessions(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_visitor_sessions_visitor_id ON visitor_sessions(visitor_id);
