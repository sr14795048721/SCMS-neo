-- =====================================================================
-- SCMS+ 全量数据库初始化脚本（由 db/init/build_init_sql.py 生成，勿手改）
-- 目标数据库：PostgreSQL 16+  库名 scms_dev  用户 scms
-- 内容：V1..V33 全部表结构 / 索引 / 约束 / 种子数据，按迁移顺序执行
-- 末尾写入 flyway_schema_history（含精确校验和），后端启动时 Flyway 校验直接通过
-- 重新生成：python db/init/build_init_sql.py
-- =====================================================================

-- ================= V1: init schema =================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    email VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS clubs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS activities (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id),
    title VARCHAR(180) NOT NULL,
    description TEXT,
    location VARCHAR(160),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    capacity INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_activities_club ON activities(club_id);

CREATE TABLE IF NOT EXISTS registrations (
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL REFERENCES activities(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    canceled_at TIMESTAMPTZ,
    CONSTRAINT uk_activity_user UNIQUE (activity_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_registrations_activity_status ON registrations(activity_id, status);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    category VARCHAR(64) NOT NULL,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    operator_id BIGINT,
    target_type VARCHAR(64),
    target_id VARCHAR(64),
    details TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);

-- ================= V2: telemetry =================
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

-- ================= V3: home banners =================
CREATE TABLE IF NOT EXISTS home_banners (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL DEFAULT '',
    description TEXT NOT NULL DEFAULT '',
    media_type VARCHAR(16) NOT NULL,
    media_path VARCHAR(255),
    media_content_type VARCHAR(128),
    media_size_bytes BIGINT,
    poster_path VARCHAR(255),
    poster_content_type VARCHAR(128),
    sort_order INTEGER NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_home_banners_sort_order ON home_banners(sort_order);

-- ================= V4: home banners scope =================
ALTER TABLE home_banners
    ADD COLUMN IF NOT EXISTS scope VARCHAR(16) NOT NULL DEFAULT 'HOME';

UPDATE home_banners
SET scope = 'HOME'
WHERE scope IS NULL
   OR scope = '';

CREATE INDEX IF NOT EXISTS idx_home_banners_scope_sort_order_id
    ON home_banners(scope, sort_order, id);

-- ================= V5: manager info =================
CREATE TABLE IF NOT EXISTS manager_info (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(120),
    manager_no VARCHAR(64),
    phone VARCHAR(32),
    bio TEXT,
    avatar_path VARCHAR(255),
    avatar_updated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_manager_info_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_manager_info_user_id ON manager_info(user_id);

-- ================= V6: student info =================
CREATE TABLE student_info (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(120),
    student_no VARCHAR(64),
    grade VARCHAR(64),
    class_name VARCHAR(120),
    phone VARCHAR(32),
    bio TEXT,
    avatar_path VARCHAR(255),
    avatar_updated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_student_info_user UNIQUE (user_id),
    CONSTRAINT fk_student_info_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- ================= V7: student grade class normalization =================
UPDATE student_info
SET grade = CASE BTRIM(grade)
    WHEN 'HIGH_1' THEN 'HIGH_1'
    WHEN 'HIGH_2' THEN 'HIGH_2'
    WHEN 'HIGH_3' THEN 'HIGH_3'
    WHEN '高一' THEN 'HIGH_1'
    WHEN '高二' THEN 'HIGH_2'
    WHEN '高三' THEN 'HIGH_3'
    ELSE NULL
END
WHERE grade IS NOT NULL;

UPDATE student_info
SET class_name = SUBSTRING(BTRIM(class_name) FROM '^([0-9]{1,3})班$')
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) ~ '^[0-9]{1,3}班$';

UPDATE student_info
SET class_name = BTRIM(class_name)
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) ~ '^[0-9]{1,3}$';

UPDATE student_info
SET class_name = NULL
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) !~ '^[0-9]{1,3}$';

-- ================= V8: student class name range limit =================
UPDATE student_info
SET class_name = NULL
WHERE class_name IS NOT NULL
  AND (
    BTRIM(class_name) !~ '^[0-9]{1,3}$'
    OR CAST(BTRIM(class_name) AS INTEGER) < 1
    OR CAST(BTRIM(class_name) AS INTEGER) > 30
  );

UPDATE student_info
SET class_name = CAST(CAST(BTRIM(class_name) AS INTEGER) AS VARCHAR(2))
WHERE class_name IS NOT NULL
  AND BTRIM(class_name) ~ '^[0-9]{1,3}$'
  AND CAST(BTRIM(class_name) AS INTEGER) BETWEEN 1 AND 30;

-- ================= V9: news articles =================
CREATE TABLE IF NOT EXISTS news_articles (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL DEFAULT '',
    markdown_content TEXT NOT NULL DEFAULT '',
    status VARCHAR(16) NOT NULL,
    cover_path VARCHAR(255),
    cover_content_type VARCHAR(128),
    cover_size_bytes BIGINT,
    cover_updated_at TIMESTAMPTZ,
    author_id BIGINT NOT NULL REFERENCES users(id),
    published_at TIMESTAMPTZ,
    view_count BIGINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_news_articles_status_published ON news_articles(status, published_at DESC, id DESC);
CREATE INDEX IF NOT EXISTS idx_news_articles_updated ON news_articles(updated_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS news_assets (
    id BIGSERIAL PRIMARY KEY,
    news_id BIGINT NOT NULL REFERENCES news_articles(id) ON DELETE CASCADE,
    asset_type VARCHAR(32) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_news_assets_news_id ON news_assets(news_id, id);

-- ================= V10: club management =================
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

-- ================= V11: home club recommendations =================
CREATE TABLE IF NOT EXISTS home_club_recommendations (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    slot_no SMALLINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_home_club_recommendations_club UNIQUE (club_id),
    CONSTRAINT uk_home_club_recommendations_slot UNIQUE (slot_no),
    CONSTRAINT chk_home_club_recommendations_slot CHECK (slot_no BETWEEN 1 AND 4)
);

CREATE INDEX IF NOT EXISTS idx_home_club_recommendations_slot ON home_club_recommendations(slot_no);
CREATE INDEX IF NOT EXISTS idx_home_club_recommendations_club ON home_club_recommendations(club_id);

-- ================= V12: activity management admin =================
ALTER TABLE activities
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE activities
SET updated_at = COALESCE(updated_at, created_at, NOW());

-- ================= V13: score reward =================
CREATE TABLE IF NOT EXISTS score_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    score_delta INTEGER NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    club_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_score_rules_status_created_at
    ON score_rules(status, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_score_rules_club_id
    ON score_rules(club_id);

CREATE TABLE IF NOT EXISTS score_records (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rule_id BIGINT,
    score_delta INTEGER NOT NULL,
    reason TEXT NOT NULL,
    operator_user_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_score_records_club_created_at
    ON score_records(club_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_score_records_user_created_at
    ON score_records(user_id, created_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_score_records_rule_id
    ON score_records(rule_id);

CREATE TABLE IF NOT EXISTS reward_items (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    score_cost INTEGER NOT NULL,
    stock INTEGER NOT NULL,
    image_path VARCHAR(255),
    image_content_type VARCHAR(120),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reward_items_status_created_at
    ON reward_items(status, created_at DESC, id DESC);

CREATE TABLE IF NOT EXISTS reward_orders (
    id BIGSERIAL PRIMARY KEY,
    reward_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score_cost INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    completed_by BIGINT,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reward_orders_reward_id
    ON reward_orders(reward_id);

CREATE INDEX IF NOT EXISTS idx_reward_orders_user_status
    ON reward_orders(user_id, status);

CREATE INDEX IF NOT EXISTS idx_reward_orders_created_at
    ON reward_orders(created_at DESC, id DESC);

-- ================= V14: manager club join requests =================
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

-- ================= V15: teacher workflows and notifications =================
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

-- ================= V16: student reward order rejection =================
ALTER TABLE reward_orders
    ADD COLUMN IF NOT EXISTS rejected_by BIGINT;

ALTER TABLE reward_orders
    ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMPTZ;

-- ================= V17: club custom duties =================
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

-- ================= V18: reward visibility scope =================
ALTER TABLE reward_items
    ADD COLUMN IF NOT EXISTS visibility_scope VARCHAR(32) NOT NULL DEFAULT 'UNASSIGNED';

CREATE TABLE IF NOT EXISTS reward_item_target_clubs (
    id BIGSERIAL PRIMARY KEY,
    reward_id BIGINT NOT NULL,
    club_id BIGINT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_reward_item_target_clubs_reward_club
    ON reward_item_target_clubs(reward_id, club_id);

CREATE INDEX IF NOT EXISTS idx_reward_item_target_clubs_reward_id
    ON reward_item_target_clubs(reward_id);

CREATE INDEX IF NOT EXISTS idx_reward_item_target_clubs_club_id
    ON reward_item_target_clubs(club_id);

UPDATE reward_items
SET visibility_scope = 'UNASSIGNED'
WHERE visibility_scope IS NULL
   OR visibility_scope <> 'UNASSIGNED';

-- ================= V19: repair club duty defaults =================
UPDATE club_duties
SET created_at = COALESCE(created_at, NOW()),
    updated_at = COALESCE(updated_at, NOW())
WHERE created_at IS NULL
   OR updated_at IS NULL;

UPDATE club_duties duty
SET name = mapped.expected_name,
    updated_at = NOW()
FROM (
    SELECT DISTINCT
        member.duty_id,
        CASE COALESCE(NULLIF(member.role, ''), 'MEMBER')
            WHEN 'PRESIDENT' THEN '社长'
            WHEN 'VICE_PRESIDENT' THEN '副社长'
            WHEN 'MINISTER' THEN '部长'
            WHEN 'SECRETARY' THEN '秘书'
            WHEN 'TREASURER' THEN '财务'
            ELSE '普通社员'
        END AS expected_name
    FROM club_student_members member
    WHERE member.duty_id IS NOT NULL
) mapped
WHERE duty.id = mapped.duty_id
  AND duty.name IS DISTINCT FROM mapped.expected_name;

-- ================= V20: enforce student membership uniqueness =================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM club_student_members
        GROUP BY student_user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate club_student_members rows found for the same student. Clean up memberships before applying V20.';
    END IF;
END
$$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM club_join_requests
        WHERE status = 'PENDING'
        GROUP BY student_user_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate pending club_join_requests rows found for the same student. Clean up join requests before applying V20.';
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_student_member_user
    ON club_student_members(student_user_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_club_join_requests_pending_student
    ON club_join_requests(student_user_id)
    WHERE status = 'PENDING';

-- ================= V21: club attendance sessions =================
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

-- ================= V22: app releases =================
CREATE TABLE IF NOT EXISTS app_releases (
    id BIGSERIAL PRIMARY KEY,
    version_name VARCHAR(32) NOT NULL,
    build_number INTEGER NOT NULL,
    release_notes TEXT NOT NULL DEFAULT '',
    force_update BOOLEAN NOT NULL DEFAULT FALSE,
    android_url VARCHAR(500),
    harmony_url VARCHAR(500),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMPTZ,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_releases_version_build
    ON app_releases(version_name, build_number);

CREATE INDEX IF NOT EXISTS idx_app_releases_status_published_at
    ON app_releases(status, published_at DESC, id DESC);

-- ================= V23: home banners media optimization =================
ALTER TABLE home_banners
    ADD COLUMN IF NOT EXISTS media_optimized_version INTEGER;

-- ================= V24: club app workspaces =================
CREATE TABLE IF NOT EXISTS club_app_workspace_groups (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES clubs(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(500) NOT NULL DEFAULT '',
    sort_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_groups_club_order
    ON club_app_workspace_groups(club_id, sort_order, id);

CREATE TABLE IF NOT EXISTS club_app_workspace_projects (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES club_app_workspace_groups(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    subtitle VARCHAR(500) NOT NULL DEFAULT '',
    cover_url VARCHAR(500),
    sort_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_projects_group_order
    ON club_app_workspace_projects(group_id, sort_order, id);

WITH target_club AS (
    SELECT id, created_by
    FROM clubs
    WHERE name = '固原市第二中学修远科技社'
),
inserted_group AS (
    INSERT INTO club_app_workspace_groups (
        club_id,
        title,
        subtitle,
        sort_order,
        enabled,
        created_by,
        updated_by
    )
    SELECT
        target_club.id,
        '第40届青少年科技创新大赛',
        '',
        1,
        TRUE,
        target_club.created_by,
        target_club.created_by
    FROM target_club
    WHERE NOT EXISTS (
        SELECT 1
        FROM club_app_workspace_groups existing_group
        WHERE existing_group.club_id = target_club.id
    )
    RETURNING id, created_by
)
INSERT INTO club_app_workspace_projects (
    group_id,
    title,
    subtitle,
    cover_url,
    sort_order,
    enabled,
    created_by,
    updated_by
)
SELECT
    inserted_group.id,
    seeded_project.title,
    seeded_project.subtitle,
    NULL,
    seeded_project.sort_order,
    TRUE,
    inserted_group.created_by,
    inserted_group.created_by
FROM inserted_group
CROSS JOIN (
    VALUES
        ('骑行守护者', 'AI驱动的智能骑行安全与健康监测系统', 1),
        ('倾语护心', '基于嵌入式边缘平台的青少年心理健康语音倾诉系统', 2),
        ('隐智管家', '基于本地记忆与云端协同的隐私优先智能管家系统', 3),
        ('耕域智测', '便携式土壤污染检测与作物智能推荐系统', 4)
) AS seeded_project(title, subtitle, sort_order);

-- ================= V25: club app workspace project details =================
ALTER TABLE club_app_workspace_projects
    ADD COLUMN IF NOT EXISTS project_key VARCHAR(120),
    ADD COLUMN IF NOT EXISTS summary VARCHAR(4000) NOT NULL DEFAULT '';

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_projects_project_key
    ON club_app_workspace_projects(project_key);

CREATE TABLE IF NOT EXISTS club_app_workspace_project_materials (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES club_app_workspace_projects(id) ON DELETE CASCADE,
    section_key VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    sort_order INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id),
    updated_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_club_app_workspace_project_materials_project_order
    ON club_app_workspace_project_materials(project_id, sort_order, id);

WITH target_projects AS (
    SELECT
        project.id,
        project.title,
        project.subtitle,
        project.created_by
    FROM club_app_workspace_projects project
    JOIN club_app_workspace_groups project_group
        ON project_group.id = project.group_id
    JOIN clubs club
        ON club.id = project_group.club_id
    WHERE club.name = '固原市第二中学修远科技社'
)
UPDATE club_app_workspace_projects project
SET
    project_key = CASE
        WHEN target_projects.title = '骑行守护者' THEN 'cycling-guardian'
        WHEN target_projects.title = '倾语护心' THEN 'qingyu-huxin'
        WHEN target_projects.title = '隐智管家' THEN 'yinzhi-guanjia'
        WHEN target_projects.title = '耕域智测' THEN 'gengyu-zhice'
        ELSE project.project_key
    END,
    summary = CASE
        WHEN project.summary = '' THEN COALESCE(project.subtitle, '')
        ELSE project.summary
    END
FROM target_projects
WHERE project.id = target_projects.id;

UPDATE club_app_workspace_projects
SET
    project_key = COALESCE(NULLIF(project_key, ''), CONCAT('project-', id)),
    summary = CASE
        WHEN summary = '' THEN COALESCE(subtitle, '')
        ELSE summary
    END
WHERE project_key IS NULL
   OR project_key = ''
   OR summary = '';

ALTER TABLE club_app_workspace_projects
    ALTER COLUMN project_key SET NOT NULL;

WITH seeded_materials(project_key, section_key, title, storage_path, sort_order) AS (
    VALUES
        ('cycling-guardian', 'OVERVIEW', '硬件清单', 'projects/cycling-guardian/overview/hardware-list.xlsx', 1),
        ('cycling-guardian', 'OVERVIEW', '设计分析', 'projects/cycling-guardian/overview/project-design-analysis.md', 2),
        ('cycling-guardian', 'OVERVIEW', '项目摘要', 'projects/cycling-guardian/overview/project-summary.txt', 3),
        ('cycling-guardian', 'ATTACHMENTS', '实验日志', 'projects/cycling-guardian/registration/attachments/experiment-log.docx', 1),
        ('cycling-guardian', 'ATTACHMENTS', '原始实验记录', 'projects/cycling-guardian/registration/attachments/original-experiment-record.docx', 2),
        ('cycling-guardian', 'IMAGES', '核心创新图', 'projects/cycling-guardian/registration/images/core-innovation-diagram.png', 1),
        ('cycling-guardian', 'IMAGES', '项目定位图', 'projects/cycling-guardian/registration/images/project-positioning-diagram.png', 2),
        ('cycling-guardian', 'IMAGES', '系统架构图', 'projects/cycling-guardian/registration/images/system-architecture-diagram.png', 3),
        ('cycling-guardian', 'POSTER', '项目海报', 'projects/cycling-guardian/registration/poster/project-poster.png', 1),
        ('cycling-guardian', 'POSTER', '海报源文件', 'projects/cycling-guardian/registration/poster/project-poster-source.psd', 2),
        ('cycling-guardian', 'REPORT', '研究报告', 'projects/cycling-guardian/registration/report/research-report.docx', 1),
        ('cycling-guardian', 'VIDEOS', '项目介绍视频', 'projects/cycling-guardian/registration/videos/project-intro.mp4', 1),
        ('cycling-guardian', 'VIDEOS', '项目介绍演示文稿', 'projects/cycling-guardian/registration/videos/project-intro-slides.pptx', 2),

        ('qingyu-huxin', 'OVERVIEW', '硬件清单', 'projects/qingyu-huxin/overview/hardware-list.xlsx', 1),
        ('qingyu-huxin', 'OVERVIEW', '设计分析', 'projects/qingyu-huxin/overview/project-design-analysis.md', 2),
        ('qingyu-huxin', 'OVERVIEW', '项目摘要', 'projects/qingyu-huxin/overview/project-summary.txt', 3),
        ('qingyu-huxin', 'ATTACHMENTS', '实验日志', 'projects/qingyu-huxin/registration/attachments/experiment-log.docx', 1),
        ('qingyu-huxin', 'ATTACHMENTS', '原始实验记录', 'projects/qingyu-huxin/registration/attachments/original-experiment-record.docx', 2),
        ('qingyu-huxin', 'IMAGES', '核心创新图', 'projects/qingyu-huxin/registration/images/core-innovation-diagram.png', 1),
        ('qingyu-huxin', 'IMAGES', '项目架构图', 'projects/qingyu-huxin/registration/images/project-architecture-diagram.png', 2),
        ('qingyu-huxin', 'IMAGES', '项目定位图', 'projects/qingyu-huxin/registration/images/project-positioning-diagram.png', 3),
        ('qingyu-huxin', 'POSTER', '项目海报', 'projects/qingyu-huxin/registration/poster/project-poster.png', 1),
        ('qingyu-huxin', 'POSTER', '海报源文件', 'projects/qingyu-huxin/registration/poster/project-poster-source.psd', 2),
        ('qingyu-huxin', 'REPORT', '研究报告', 'projects/qingyu-huxin/registration/report/research-report.docx', 1),
        ('qingyu-huxin', 'VIDEOS', '项目介绍视频', 'projects/qingyu-huxin/registration/videos/project-intro.mp4', 1),

        ('yinzhi-guanjia', 'OVERVIEW', '设计分析', 'projects/yinzhi-guanjia/overview/project-design-analysis.md', 1),
        ('yinzhi-guanjia', 'OVERVIEW', '项目摘要', 'projects/yinzhi-guanjia/overview/project-summary.txt', 2),
        ('yinzhi-guanjia', 'ATTACHMENTS', '实验日志', 'projects/yinzhi-guanjia/registration/attachments/experiment-log.docx', 1),
        ('yinzhi-guanjia', 'ATTACHMENTS', '原始实验记录', 'projects/yinzhi-guanjia/registration/attachments/original-experiment-record.docx', 2),
        ('yinzhi-guanjia', 'IMAGES', '核心创新图', 'projects/yinzhi-guanjia/registration/images/core-innovation-diagram.png', 1),
        ('yinzhi-guanjia', 'IMAGES', '项目架构图', 'projects/yinzhi-guanjia/registration/images/project-architecture-diagram.png', 2),
        ('yinzhi-guanjia', 'IMAGES', '项目定位图', 'projects/yinzhi-guanjia/registration/images/project-positioning-diagram.png', 3),
        ('yinzhi-guanjia', 'POSTER', '项目海报', 'projects/yinzhi-guanjia/registration/poster/project-poster.png', 1),
        ('yinzhi-guanjia', 'POSTER', '海报源文件', 'projects/yinzhi-guanjia/registration/poster/project-poster-source.psd', 2),
        ('yinzhi-guanjia', 'REPORT', '研究报告', 'projects/yinzhi-guanjia/registration/report/research-report.docx', 1),
        ('yinzhi-guanjia', 'VIDEOS', '项目介绍视频', 'projects/yinzhi-guanjia/registration/videos/project-intro.mp4', 1),

        ('gengyu-zhice', 'OVERVIEW', '硬件清单', 'projects/gengyu-zhice/overview/hardware-list.xlsx', 1),
        ('gengyu-zhice', 'OVERVIEW', '设计分析', 'projects/gengyu-zhice/overview/project-design-analysis.md', 2),
        ('gengyu-zhice', 'OVERVIEW', '项目摘要', 'projects/gengyu-zhice/overview/project-summary.txt', 3),
        ('gengyu-zhice', 'ATTACHMENTS', '实验日志', 'projects/gengyu-zhice/registration/attachments/experiment-log.docx', 1),
        ('gengyu-zhice', 'ATTACHMENTS', '原始实验记录', 'projects/gengyu-zhice/registration/attachments/original-experiment-record.docx', 2),
        ('gengyu-zhice', 'IMAGES', '核心创新图', 'projects/gengyu-zhice/registration/images/core-innovation-diagram.png', 1),
        ('gengyu-zhice', 'IMAGES', '项目定位图', 'projects/gengyu-zhice/registration/images/project-positioning-diagram.png', 2),
        ('gengyu-zhice', 'IMAGES', '系统总览图', 'projects/gengyu-zhice/registration/images/system-overview-diagram.png', 3),
        ('gengyu-zhice', 'POSTER', '项目海报', 'projects/gengyu-zhice/registration/poster/project-poster.png', 1),
        ('gengyu-zhice', 'POSTER', '海报源文件', 'projects/gengyu-zhice/registration/poster/project-poster-source.psd', 2),
        ('gengyu-zhice', 'REPORT', '研究报告', 'projects/gengyu-zhice/registration/report/research-report.docx', 1),
        ('gengyu-zhice', 'VIDEOS', '项目介绍视频', 'projects/gengyu-zhice/registration/videos/project-intro.mp4', 1)
),
target_projects AS (
    SELECT
        project.id,
        project.project_key,
        project.created_by
    FROM club_app_workspace_projects project
    JOIN club_app_workspace_groups project_group
        ON project_group.id = project.group_id
    JOIN clubs club
        ON club.id = project_group.club_id
    WHERE club.name = '固原市第二中学修远科技社'
)
INSERT INTO club_app_workspace_project_materials (
    project_id,
    section_key,
    title,
    storage_path,
    sort_order,
    enabled,
    created_by,
    updated_by
)
SELECT
    target_projects.id,
    seeded_materials.section_key,
    seeded_materials.title,
    seeded_materials.storage_path,
    seeded_materials.sort_order,
    TRUE,
    target_projects.created_by,
    target_projects.created_by
FROM target_projects
JOIN seeded_materials
    ON seeded_materials.project_key = target_projects.project_key
WHERE NOT EXISTS (
    SELECT 1
    FROM club_app_workspace_project_materials material
    WHERE material.project_id = target_projects.id
);

-- ================= V26: club app workspace project demos =================
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

-- ================= V27: drop club app workspace project demo devices =================
DROP TABLE IF EXISTS club_app_workspace_project_demo_devices;

-- ================= V28: project demo step action keys =================
ALTER TABLE club_app_workspace_project_demo_steps
    ADD COLUMN IF NOT EXISTS action_key VARCHAR(120);

UPDATE club_app_workspace_project_demo_steps AS step
SET action_key = 'qingyu-huxin-esp32-provision'
FROM club_app_workspace_project_demo_profiles AS profile
         JOIN club_app_workspace_projects AS project
              ON project.id = profile.project_id
WHERE step.demo_profile_id = profile.id
  AND project.project_key = 'qingyu-huxin'
  AND step.sort_order = 1
  AND LOWER(COALESCE(step.target_subsystem, '')) LIKE '%app%'
  AND (step.action_key IS NULL OR BTRIM(step.action_key) = '');

-- ================= V29: qingyu huxin risk step action key =================
UPDATE club_app_workspace_project_demo_steps AS step
SET target_subsystem = 'APP',
    action_key = 'qingyu-huxin-risk-review'
FROM club_app_workspace_project_demo_profiles AS profile
         JOIN club_app_workspace_projects AS project
              ON project.id = profile.project_id
WHERE step.demo_profile_id = profile.id
  AND project.project_key = 'qingyu-huxin'
  AND step.sort_order = 5;

-- ================= V30: iot device telemetry =================
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

-- ================= V31: yinzhi ai inspection =================
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

-- ================= V32: iot sequence reset history =================
ALTER TABLE iot_device_telemetry_events
    DROP CONSTRAINT IF EXISTS uq_iot_device_telemetry_events_device_sequence;

CREATE INDEX IF NOT EXISTS idx_iot_device_telemetry_events_device_sequence_received
    ON iot_device_telemetry_events(device_id, upload_sequence, received_at DESC, id DESC);

ALTER TABLE yinzhi_ai_inspection_runs
    DROP CONSTRAINT IF EXISTS uq_yinzhi_ai_inspection_runs_device_sequence;

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_inspection_runs_device_sequence_created
    ON yinzhi_ai_inspection_runs(device_id, upload_sequence, created_at DESC, id DESC);

-- ================= V33: yinzhi command closed loop verification =================
ALTER TABLE yinzhi_ai_device_commands
    ADD COLUMN IF NOT EXISTS verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verification_telemetry_event_id BIGINT,
    ADD COLUMN IF NOT EXISTS verification_upload_sequence BIGINT,
    ADD COLUMN IF NOT EXISTS verification_message VARCHAR(1200);

CREATE INDEX IF NOT EXISTS idx_yinzhi_ai_device_commands_verification
    ON yinzhi_ai_device_commands(device_id, status, upload_sequence ASC, id ASC);

-- ================= flyway_schema_history =================
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    "installed_rank" INT NOT NULL,
    "version" VARCHAR(50),
    "description" VARCHAR(200) NOT NULL,
    "type" VARCHAR(20) NOT NULL,
    "script" VARCHAR(1000) NOT NULL,
    "checksum" INT,
    "installed_by" VARCHAR(100) NOT NULL,
    "installed_on" TIMESTAMP DEFAULT now() NOT NULL,
    "execution_time" INT NOT NULL,
    "success" BOOLEAN NOT NULL,
    CONSTRAINT flyway_schema_history_pk PRIMARY KEY ("installed_rank")
);
CREATE INDEX IF NOT EXISTS flyway_schema_history_s_idx ON flyway_schema_history ("success");

INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (1, '1', 'init schema', 'SQL', 'V1__init_schema.sql', -1140591364, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (2, '2', 'telemetry', 'SQL', 'V2__telemetry.sql', 301435666, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (3, '3', 'home banners', 'SQL', 'V3__home_banners.sql', 386347159, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (4, '4', 'home banners scope', 'SQL', 'V4__home_banners_scope.sql', 811390145, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (5, '5', 'manager info', 'SQL', 'V5__manager_info.sql', -1788858879, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (6, '6', 'student info', 'SQL', 'V6__student_info.sql', 1462081196, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (7, '7', 'student grade class normalization', 'SQL', 'V7__student_grade_class_normalization.sql', -1960230823, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (8, '8', 'student class name range limit', 'SQL', 'V8__student_class_name_range_limit.sql', 736483362, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (9, '9', 'news articles', 'SQL', 'V9__news_articles.sql', -682786464, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (10, '10', 'club management', 'SQL', 'V10__club_management.sql', 1290792937, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (11, '11', 'home club recommendations', 'SQL', 'V11__home_club_recommendations.sql', 1389529519, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (12, '12', 'activity management admin', 'SQL', 'V12__activity_management_admin.sql', 663669551, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (13, '13', 'score reward', 'SQL', 'V13__score_reward.sql', -1644038191, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (14, '14', 'manager club join requests', 'SQL', 'V14__manager_club_join_requests.sql', 630408883, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (15, '15', 'teacher workflows and notifications', 'SQL', 'V15__teacher_workflows_and_notifications.sql', -2118291462, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (16, '16', 'student reward order rejection', 'SQL', 'V16__student_reward_order_rejection.sql', -371689976, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (17, '17', 'club custom duties', 'SQL', 'V17__club_custom_duties.sql', 1712264006, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (18, '18', 'reward visibility scope', 'SQL', 'V18__reward_visibility_scope.sql', 1166900990, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (19, '19', 'repair club duty defaults', 'SQL', 'V19__repair_club_duty_defaults.sql', -1015694557, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (20, '20', 'enforce student membership uniqueness', 'SQL', 'V20__enforce_student_membership_uniqueness.sql', -1921021986, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (21, '21', 'club attendance sessions', 'SQL', 'V21__club_attendance_sessions.sql', -196433851, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (22, '22', 'app releases', 'SQL', 'V22__app_releases.sql', -796144010, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (23, '23', 'home banners media optimization', 'SQL', 'V23__home_banners_media_optimization.sql', -286084142, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (24, '24', 'club app workspaces', 'SQL', 'V24__club_app_workspaces.sql', 111070626, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (25, '25', 'club app workspace project details', 'SQL', 'V25__club_app_workspace_project_details.sql', -552224492, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (26, '26', 'club app workspace project demos', 'SQL', 'V26__club_app_workspace_project_demos.sql', -715757209, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (27, '27', 'drop club app workspace project demo devices', 'SQL', 'V27__drop_club_app_workspace_project_demo_devices.sql', -1579218042, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (28, '28', 'project demo step action keys', 'SQL', 'V28__project_demo_step_action_keys.sql', 1132057156, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (29, '29', 'qingyu huxin risk step action key', 'SQL', 'V29__qingyu_huxin_risk_step_action_key.sql', 1802766247, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (30, '30', 'iot device telemetry', 'SQL', 'V30__iot_device_telemetry.sql', -567640866, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (31, '31', 'yinzhi ai inspection', 'SQL', 'V31__yinzhi_ai_inspection.sql', 397861786, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (32, '32', 'iot sequence reset history', 'SQL', 'V32__iot_sequence_reset_history.sql', -1873468620, current_user, 100, true);
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (33, '33', 'yinzhi command closed loop verification', 'SQL', 'V33__yinzhi_command_closed_loop_verification.sql', -528664608, current_user, 100, true);
