CREATE TABLE IF NOT EXISTS attendance_sessions (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id),
    title VARCHAR(120) NOT NULL,
    score_rule_id BIGINT NOT NULL REFERENCES score_rules(id),
    created_by BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ended_at TIMESTAMPTZ,
    settled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_attendance_sessions_club_created
    ON attendance_sessions(club_id, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS attendance_records (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES attendance_sessions(id) ON DELETE CASCADE,
    student_user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    check_in_at TIMESTAMPTZ,
    check_out_at TIMESTAMPTZ,
    settled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_attendance_session_student UNIQUE (session_id, student_user_id)
);

CREATE INDEX IF NOT EXISTS idx_attendance_records_session
    ON attendance_records(session_id, created_at ASC, id ASC);

ALTER TABLE score_records
    ADD COLUMN IF NOT EXISTS attendance_session_id BIGINT REFERENCES attendance_sessions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_score_records_attendance_session
    ON score_records(attendance_session_id);
