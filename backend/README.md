# SCMS+ Backend (2C2G Lightweight Edition)

SCMS+ is a lightweight microservice backend for a student club management system optimized for low-resource development machines (2 CPU / 2 GB memory).

## Services

- `gateway-service` (`8080`): unified API entry and JWT pre-check.
- `core-service` (`8081`): auth, user, club, activity, registration, notification, audit, report.
- `file-service` (`8082`, stage 2): MinIO-based file upload/download.

## Stage 1 Quick Start

1. Build (optional if you use `docker compose ... --build`):

```bash
./mvnw -q -DskipTests clean package
```

2. Run base stack:

```bash
docker compose up -d --build
```

If Maven dependency download appears to hang during image build, use plain logs and sequential build:

```bash
docker compose build --no-parallel --progress plain
docker compose up -d
```

The development database name is `scms_dev` (inside Docker Postgres).
To avoid conflicts with a locally installed PostgreSQL instance, the host port is mapped to `5433`.

Uploaded reward images are persisted on the host under `backend/data/core-reward-images` via the Docker volume mount for `core-service`.

3. Health checks:

- Gateway: `http://localhost:8080/actuator/health`
- Core: `http://localhost:8081/actuator/health`

4. Default account:

- `admin / admin123`
- `manager / manager123`
- `student / student123`
- `admin@scms.local`
- `manager@scms.local`
- `student@scms.local`

Default users are controlled by `SCMS_BOOTSTRAP_DEFAULT_USERS` (default `true` for local development).

5. Optional smoke test:

```powershell
./scripts/smoke.ps1
```

## Reset Development Data

Use this only in local development:

```bash
docker compose down -v
docker compose up -d --build
```

## Stage 2 Optional Components

Run extra middleware and file service:

```bash
docker compose -f docker-compose.yml -f docker-compose.stage2.yml up -d --build
```

## Stage 3 Optional Observability

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
```

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (`admin/admin`)

## API Prefix

All API endpoints use `/api/v1/**`.

## Unified Error Response

All backend errors (gateway + core) use a unified shape:

```json
{
  "code": "UNAUTHORIZED",
  "message": "missing bearer token",
  "data": null,
  "requestId": "9ebcd763-63e0-4c0d-aa02-63dbb1ef8458",
  "timestamp": "2026-03-30T06:00:00Z"
}
```

## Production Safety

- Disable default account bootstrap in production:

```bash
SCMS_BOOTSTRAP_DEFAULT_USERS=false
```

- Or run with `prod` profile (which sets bootstrap disabled):

```bash
SPRING_PROFILES_ACTIVE=prod
```

## Production First Admin Initialization

Production does not create default `admin / admin123` accounts.

To safely create the first administrator once, start `core-service` with:

```bash
SPRING_PROFILES_ACTIVE=prod
SCMS_BOOTSTRAP_DEFAULT_USERS=false
SCMS_ADMIN_INIT_ENABLED=true
SCMS_ADMIN_INIT_USERNAME=your_admin
SCMS_ADMIN_INIT_EMAIL=admin@example.com
SCMS_ADMIN_INIT_PASSWORD=replace-with-a-strong-random-password
```

Behavior:

- The initializer only runs when `SCMS_ADMIN_INIT_ENABLED=true`.
- It only creates an admin when the database currently has no `ADMIN` user.
- It refuses to start if the configured username or email already exists.
- It never logs the plaintext password.

After the first admin is created:

1. Sign in with the initialized administrator account.
2. Remove `SCMS_ADMIN_INIT_*` from the environment.
3. Restart the service.

Use this mechanism only for the first production administrator. All later administrators should be created from the admin console.

## Production Hidden Super Administrator Initialization

If you need a hidden `SUPER_ADMIN` account for top-level system testing, start `core-service` with:

```bash
SPRING_PROFILES_ACTIVE=prod
SCMS_SUPER_ADMIN_INIT_ENABLED=true
SCMS_SUPER_ADMIN_INIT_USERNAME=your_super_admin
SCMS_SUPER_ADMIN_INIT_EMAIL=superadmin@example.com
SCMS_SUPER_ADMIN_INIT_PASSWORD=replace-with-a-strong-random-password
```

Behavior:

- The initializer only runs when `SCMS_SUPER_ADMIN_INIT_ENABLED=true`.
- It only creates a super administrator when the database currently has no `SUPER_ADMIN` user.
- It refuses to start if the configured username or email already exists.
- It never logs the plaintext password.

After the hidden super administrator is created:

1. Sign in with the initialized super administrator account.
2. Remove `SCMS_SUPER_ADMIN_INIT_*` from the environment.
3. Restart the service.

## Observability (Lightweight)

- Metrics: Prometheus endpoint via `/actuator/prometheus`
- Logs: standard JSON-like console logging

## Project Structure

```text
.
├── core-service
├── gateway-service
├── file-service
├── deploy
│   ├── helm
│   └── k8s
└── docs
```

## Docs

- `docs/architecture.md`
- `docs/roadmap.md`
- `docs/runbook.md`
- `docs/api-examples.http`
