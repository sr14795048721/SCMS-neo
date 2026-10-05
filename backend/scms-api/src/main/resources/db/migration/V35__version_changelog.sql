-- Version changelog: admins maintain release notes visible in the admin and
-- club-manager consoles.

CREATE TABLE IF NOT EXISTS version_changelogs (
    id BIGSERIAL PRIMARY KEY,
    version VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    released_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_version_changelogs_version
    ON version_changelogs(version);

INSERT INTO version_changelogs (version, title, content, released_at) VALUES
('v0.2.0', '修远科技社独立品牌与积分明细', E'新增：学生积分明细，可查看每一笔积分的来源与经手人\n修复：公开签到对后加入成员自动补建签到记录\n体验：站点品牌更新为修远科技社', '2026-10-05T00:00:00Z'),
('v0.1.1', '积分管理与考勤重构', E'新增：管理员可直接调分、撤销积分记录、查看全系统积分流水\n新增：干部可撤销本人记录的积分\n新增：公开签名链接签到，学生扫码查看场次并签到', '2026-10-04T00:00:00Z'),
('v0.1.0', '单体化重构', E'架构：合并网关/核心服务为单体 scms-api\n数据库：统一 PostgreSQL，移除 Redis 依赖', '2026-10-02T00:00:00Z')
ON CONFLICT (version) DO NOTHING;
