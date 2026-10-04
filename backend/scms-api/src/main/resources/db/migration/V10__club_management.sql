ALTER TABLE clubs
    ADD COLUMN IF NOT EXISTS type VARCHAR(64),
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE clubs
SET status = COALESCE(NULLIF(status, ''), 'ACTIVE'),
    updated_at = COALESCE(updated_at, created_at, NOW());

CREATE TABLE IF NOT EXISTS club_manager_bindings (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    manager_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_club_manager_binding UNIQUE (club_id, manager_user_id)
);

CREATE INDEX IF NOT EXISTS idx_club_manager_bindings_club ON club_manager_bindings(club_id, manager_user_id);
CREATE INDEX IF NOT EXISTS idx_club_manager_bindings_manager ON club_manager_bindings(manager_user_id, club_id);

CREATE TABLE IF NOT EXISTS club_student_members (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    student_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_club_student_member UNIQUE (club_id, student_user_id)
);

CREATE INDEX IF NOT EXISTS idx_club_student_members_club ON club_student_members(club_id, student_user_id);
CREATE INDEX IF NOT EXISTS idx_club_student_members_student ON club_student_members(student_user_id, club_id);
