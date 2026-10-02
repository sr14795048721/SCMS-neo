ALTER TABLE news_articles
    ADD COLUMN IF NOT EXISTS author_role VARCHAR(32) NOT NULL DEFAULT 'ADMIN';

UPDATE news_articles
SET author_role = 'ADMIN'
WHERE author_role IS NULL;

ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS target_path VARCHAR(255);

UPDATE notifications
SET status = 'UNREAD'
WHERE status IN ('SENT', 'PENDING');

CREATE TABLE IF NOT EXISTS teacher_club_recommendations (
    id BIGSERIAL PRIMARY KEY,
    manager_user_id BIGINT NOT NULL REFERENCES users(id),
    club_id BIGINT NOT NULL REFERENCES clubs(id),
    order_no SMALLINT NOT NULL,
    remark VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_teacher_club_recommendations_manager_club UNIQUE (manager_user_id, club_id),
    CONSTRAINT uk_teacher_club_recommendations_manager_order UNIQUE (manager_user_id, order_no)
);

CREATE INDEX IF NOT EXISTS idx_teacher_club_recommendations_manager
    ON teacher_club_recommendations(manager_user_id);

CREATE TABLE IF NOT EXISTS club_creation_requests (
    id BIGSERIAL PRIMARY KEY,
    applicant_manager_user_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(120) NOT NULL,
    type VARCHAR(64) NOT NULL,
    description TEXT,
    apply_reason TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_creation_requests_applicant
    ON club_creation_requests(applicant_manager_user_id);

CREATE INDEX IF NOT EXISTS idx_club_creation_requests_status
    ON club_creation_requests(status);

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_creation_requests_pending_applicant
    ON club_creation_requests(applicant_manager_user_id)
    WHERE status = 'PENDING';
