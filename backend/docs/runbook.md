# Local Runbook (2C2G)

## Stage 1 (Core + Gateway)

1. Build jars (optional if you use docker compose build):

```bash
./mvnw -DskipTests clean package
```

2. Start stack:

```bash
docker compose up -d --build
```

Development database:

- PostgreSQL DB name: `scms_dev`

For production-like runs, disable default test accounts:

```bash
SCMS_BOOTSTRAP_DEFAULT_USERS=false
```

3. Check:

- `GET http://localhost:8080/actuator/health`
- `POST http://localhost:8080/api/v1/auth/login`
- `POST http://localhost:8080/api/v1/auth/register`

## Reset Development Database

For local-only reset of test data:

```bash
docker compose down -v
docker compose up -d --build
```

## Stage 2 (File + MinIO + Kafka)

```bash
docker compose -f docker-compose.yml -f docker-compose.stage2.yml up -d --build
```
