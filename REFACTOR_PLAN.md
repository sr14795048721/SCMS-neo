# SCMS+ 单体化重构方案

> 日期：2026-10-02
> 状态：待批准
> 背景：服务器仅 2核/1.6GB 内存（swap 已用约 2GB），现有微服务架构（网关 + 业务服务双 JVM + 独立缓存）在该规模下属于过度设计。本次重构目标：**推翻脚手架、保留业务壳**，把后端合并为单体，容器从 5 个精简到 3 个。

---

## 一、架构变化

### 现状（5 容器）

```text
nginx（不动）
   └─► scms-plus   Next.js 前端        :10056
          └─► scms-gateway  网关       :8080（容器内）
                 └─► scms-core     业务服务  :8081
                        ├─► scms-postgres  PostgreSQL :5433
                        └─► scms-redis     Redis
```

### 目标（3 容器）

```text
nginx（配置不动）
   └─► scms-web   Next.js 前端          :10056
          └─► scms-api   Spring Boot 单体  :8080（容器内）
                 └─► scms-db       PostgreSQL  （原数据卷）
```

- 网关容器移除：路由由 nginx 承担（2026-10 已完成的 nginx 修复恰好就是目标形态），鉴权过滤器并入应用
- Redis 容器移除：仅用于存 refresh token 与登出黑名单，改为进程内实现
- file-service 移除：前端零引用的死代码，直接丢弃

---

## 二、技术栈（重构后定版）

| 层 | 选型 | 说明 |
|---|---|---|
| 前端 | Next.js 14 + TypeScript + CSS Modules | 现有整套保留，不重构 |
| 后端 | Java 21 + Spring Boot 3.3 单体（Maven 单模块） | 合并 Gateway + Core，去除 Spring Cloud 依赖 |
| 鉴权 | Spring Security + JWT | `JwtGatewayFilter` 逻辑平移为应用内过滤器 |
| 会话 | 进程内 TokenStore（Caffeine / ConcurrentHashMap） | 替代 Redis；保留接口抽象，日后可切回 Redis 实现 |
| ORM / 迁移 | Spring Data JPA（现有）+ Flyway | 数据库 schema 从此版本化，禁止手工改表 |
| 数据库 | PostgreSQL 16（Docker 容器） | 数据卷原样保留，业务表结构不变 |
| 部署 | Docker Compose 3 服务 + 现有 nginx | 端口、域名、宝塔、教育平台全部不动 |

### 备选方案（不推荐首发）

NestJS 全栈 TypeScript：前后端一门语言、对新手友好，但业务逻辑需全部重写，工作量约为单体化方案的数倍，回归风险高。除非团队决定全面转向 Node，否则不采用。

---

## 三、砍什么 / 留什么

### 砍掉

- `gateway-service` 容器（路由归 nginx，鉴权并入应用）
- Redis 容器（内存实现替代）
- `file-service` 模块（死代码）
- Spring Cloud 相关依赖（网关、负载均衡等脚手架）

### 保留（壳）

- Next.js 前端整套（登录 / i18n / 布局 / 组件）
- 数据库全部业务表与现有数据
- `ApiResponse` 统一包装、JWT 双 token 设计、角色体系（ADMIN / MANAGER / STUDENT）
- nginx 配置、域名、端口 10056
- 服务器上其他一切（宝塔、教育平台、全局 MySQL/Redis）

---

## 四、目标代码结构

```text
xiuyuan/
├── frontend/                  # 现有，不动
├── backend/
│   └── scms-api/              # 单体（原 core-service + 网关鉴权合并）
│       └── src/main/java/com/scms/
│           ├── common/        # ApiResponse、全局异常（现有平移）
│           ├── security/      # JwtFilter(原网关) + JwtService + TokenStore
│           ├── user/          # 按业务域分包
│           ├── club/
│           ├── activity/
│           ├── score/
│           ├── reward/
│           └── news/
│       └── src/main/resources/db/migration/   # Flyway 迁移脚本
├── docker-compose.yml         # 3 服务：web + api + db
├── Dockerfile.api
└── Dockerfile.web
```

---

## 五、实施步骤

> 原则：全程在本地开发验证，只有第 4 步接触服务器，且只替换 SCMS 自己的容器。

| # | 内容 | 位置 |
|---|---|---|
| 1 | 建单体骨架，整体搬运 core-service 业务代码，删除 file-service | 本地 |
| 2 | 网关 `JwtGatewayFilter` 改写为应用内 Spring Security 过滤器；定义 `TokenStore` 接口 + 内存实现替换 Redis | 本地 |
| 3 | compose 精简为 3 服务，本地 `docker compose up` 全链路自测（登录、各角色接口、反馈邮件） | 本地 |
| 4 | 服务器切换：备份现 compose → 起新 3 容器（同端口）→ 验证 → 停旧 5 容器。教育平台无感知 | 服务器（唯一一步，切换前需再次批准） |
| 5 | 观察 24h，有问题一键回滚旧 compose | 服务器 |

### 内存账

5 容器实测约 200MB → 3 容器估算约 180MB。净省不多，但少养一个 JVM、少两个容器、少一跳网关转发，部署复杂度显著下降。

---

## 六、风险与回滚

| 风险 | 对策 |
|---|---|
| 数据丢失 | 数据库与数据卷完全不动，零风险 |
| 切换失败 | 旧 compose 文件与旧镜像保留，24h 内一条命令回滚 |
| 重演登录事故 | nginx 完全不动 |
| 停机影响用户 | 选择无人时段切换，停机 < 2 分钟 |

---

## 七、给协作同事的环境要求

- 前端向：Node.js 18+、npm
- 后端向：JDK 21、Maven、Docker Desktop（本地起 PostgreSQL 即可，不再需要 Redis）
- 通用：GitHub 账号 + SCMS-neo 仓库协作者权限；数据库 schema 变更一律走 Flyway 脚本，禁止手工改表
