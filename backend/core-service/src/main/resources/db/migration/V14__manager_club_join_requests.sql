ALTER TABLE club_student_members
    ADD COLUMN IF NOT EXISTS role VARCHAR(32) NOT NULL DEFAULT 'MEMBER';

UPDATE club_student_members
SET role = COALESCE(NULLIF(role, ''), 'MEMBER');

CREATE TABLE IF NOT EXISTS club_join_requests (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    student_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(500),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_join_requests_club ON club_join_requests(club_id, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_club_join_requests_student ON club_join_requests(student_user_id, status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uk_club_join_requests_pending
    ON club_join_requests(club_id, student_user_id)
    WHERE status = 'PENDING';
