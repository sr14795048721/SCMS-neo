CREATE TABLE IF NOT EXISTS club_duties (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_duties_name_lower
    ON club_duties (club_id, LOWER(name));

CREATE INDEX IF NOT EXISTS idx_club_duties_club
    ON club_duties (club_id, created_at, id);

CREATE TABLE IF NOT EXISTS club_duty_permissions (
    duty_id BIGINT NOT NULL REFERENCES club_duties(id) ON DELETE CASCADE,
    permission VARCHAR(64) NOT NULL,
    PRIMARY KEY (duty_id, permission)
);

ALTER TABLE club_student_members
    ADD COLUMN IF NOT EXISTS duty_id BIGINT REFERENCES club_duties(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_club_student_members_duty
    ON club_student_members (duty_id);

INSERT INTO club_duties (club_id, name, created_at, updated_at)
SELECT DISTINCT
    member.club_id,
    CASE COALESCE(NULLIF(member.role, ''), 'MEMBER')
        WHEN 'PRESIDENT' THEN '社长'
        WHEN 'VICE_PRESIDENT' THEN '副社长'
        WHEN 'MINISTER' THEN '部长'
        WHEN 'SECRETARY' THEN '秘书'
        WHEN 'TREASURER' THEN '财务'
        ELSE '普通社员'
    END,
    NOW(),
    NOW()
FROM club_student_members member
ON CONFLICT DO NOTHING;

UPDATE club_student_members member
SET duty_id = duty.id
FROM club_duties duty
WHERE duty.club_id = member.club_id
  AND duty.name = CASE COALESCE(NULLIF(member.role, ''), 'MEMBER')
        WHEN 'PRESIDENT' THEN '社长'
        WHEN 'VICE_PRESIDENT' THEN '副社长'
        WHEN 'MINISTER' THEN '部长'
        WHEN 'SECRETARY' THEN '秘书'
        WHEN 'TREASURER' THEN '财务'
        ELSE '普通社员'
    END
  AND member.duty_id IS NULL;
