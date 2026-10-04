# SCMS+ 数据库设计与初始化指南

- 数据库：PostgreSQL 16+，库名 `scms_dev`，用户/密码 `scms` / `scms`
- 结构来源：`scms-api/src/main/resources/db/migration/` 的 Flyway 迁移链 `V1..V33`（后端以 `ddl-auto=validate` 启动，表结构必须与 JPA 实体完全一致，迁移链即为唯一权威定义）
- 规模：41 张业务表（40 个 JPA 实体 + 1 张 `@ElementCollection` 集合表 `club_duty_permissions`），含索引、约束与业务种子数据（社团工作台项目、职责任免等）

## 快速开始

同一数据库只能选择下列**一种**初始化方式，混用会导致表冲突。

### 方式 A：Docker 一键初始化（推荐）

`docker-compose.yml` 已把 `db/init` 挂载到 postgres 容器的 `/docker-entrypoint-initdb.d`，首次创建 `pg_data` 数据卷时会自动执行 [schema_full.sql](init/schema_full.sql)（全量建表 + 种子数据 + flyway 历史记录）：

```bash
cd backend
docker compose up -d postgres
```

随后启动 `api` 服务（`docker compose up -d api`），Flyway 校验历史记录与校验和全部匹配，直接通过。

> 注意：官方 postgres 镜像只在数据卷**首次**初始化时执行该目录。若 `pg_data` 卷已存在（旧库/空库），需 `docker compose down -v` 删除卷后重启才会重新初始化。

### 方式 B：手动导入（本地 PostgreSQL / 任意 GUI 工具）

```bash
psql -h localhost -p 5432 -U scms -d scms_dev -f db/init/schema_full.sql
```

脚本自带 `ON CONFLICT`/幂等性来自迁移本身，请在**空库**中一次性执行。

### 方式 C：纯 Flyway 自动迁移（不导任何脚本）

对一个全新的空库直接启动后端，Flyway 会按 `V1..V33` 自动完成建库：

```bash
cd backend/scms-api
mvn spring-boot:run   # DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD 见 application.yml
```

## 默认账号说明

初始用户**不在 SQL 脚本中**，由后端启动引导：`SCMS_BOOTSTRAP_DEFAULT_USERS=true`（docker-compose 默认开启）在应用启动时写入默认账号；超级管理员经 `SCMS_SUPER_ADMIN_INIT_*` 环境变量开启。

## 维护：新增/修改迁移后重新生成脚本

[schema_full.sql](init/schema_full.sql) 是生成物，请勿手改：

```bash
python db/init/build_init_sql.py
# 本机若无全局 Python，可用项目自带：
# g:\kgwl.fun\tools-src\backend\venv\Scripts\python.exe db/init/build_init_sql.py
```

生成器会把每个迁移的**精确 Flyway 校验和**写入 `flyway_schema_history`（算法与 Flyway 9.x/10.x 一致：逐行 UTF-8 CRC32、行尾无关），保证后端 `flyway validate` 通过。

## 表清单（按业务模块）

| 模块 | 表 |
| --- | --- |
| 用户与身份 | `users`、`student_info`、`manager_info` |
| 社团管理 | `clubs`、`club_student_members`、`club_manager_bindings`、`club_join_requests`、`club_duties`、`club_duty_permissions`、`club_creation_requests` |
| 活动与签到 | `activities`、`registrations`、`attendance_sessions`、`attendance_records` |
| 积分与奖励 | `score_rules`、`score_records`、`reward_items`、`reward_item_target_clubs`、`reward_orders` |
| 门户内容 | `news_articles`、`news_assets`、`home_banners`、`home_club_recommendations`、`teacher_club_recommendations` |
| 通知与审计 | `notifications`、`audit_logs` |
| 访客统计 | `visitor_sessions`、`visit_events` |
| 应用发布 | `app_releases` |
| IoT 遥测 | `iot_device_telemetry_latest_states`、`iot_device_telemetry_events` |
| 隐智 AI 巡检 | `yinzhi_ai_inspection_runs`、`yinzhi_ai_inspection_logs`、`yinzhi_ai_device_commands` |
| 社团 App 工作台 | `club_app_workspace_groups`、`club_app_workspace_projects`、`club_app_workspace_project_materials`、`club_app_workspace_project_demo_profiles`、`club_app_workspace_project_demo_steps`、`club_app_workspace_project_demo_runtime_snapshots`、`club_app_workspace_project_demo_events` |

核心关系（示例）：

- `activities.club_id → clubs.id`；`registrations(activity_id, student_user_id)`
- `club_student_members(club_id, student_user_id)` 唯一，`duty_id → club_duties.id`；入社申请走 `club_join_requests`（PENDING 状态部分唯一索引防重复申请）
- `attendance_records(session_id, student_user_id)` 唯一，`score_records.attendance_session_id` 关联签到
- `reward_orders(item_id, student_user_id)`，定向奖励经 `reward_item_target_clubs`
- `visitor_sessions(session_id)` 唯一（后端原生 SQL upsert），`visit_events` 记录明细
- 工作台层级：`groups → projects → materials`，演示链路：`demo_profiles → demo_steps / demo_runtime_snapshots / demo_events`
- IoT：`iot_device_telemetry_events(device_id, upload_sequence)`，最新状态存 `iot_device_telemetry_latest_states`；AI 巡检命令闭环校验引用遥测事件（V33）

## 文件清单

- [init/schema_full.sql](init/schema_full.sql) — 全量建库脚本（生成物）
- [init/build_init_sql.py](init/build_init_sql.py) — 生成器
