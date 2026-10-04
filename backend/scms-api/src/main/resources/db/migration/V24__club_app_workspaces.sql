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
