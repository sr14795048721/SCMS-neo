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
