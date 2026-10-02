# SCMS+ Backend Architecture (Lightweight)

```mermaid
flowchart LR
  FE["Next.js Frontend"] --> GW["gateway-service"]
  GW --> CORE["core-service"]
  GW --> FILE["file-service (stage2)"]
  CORE --> PG["PostgreSQL"]
  CORE --> REDIS["Redis"]
  CORE --> KAFKA["Kafka (stage2)"]
  FILE --> MINIO["MinIO (stage2)"]
  CORE --> PROM["Prometheus scrape"]
  GW --> PROM
  FILE --> PROM
```

## Stage Mapping

- Stage 1: `gateway + core + postgres + redis`
- Stage 2: add `file + minio + kafka`
- Stage 3: add `prometheus + grafana + helm/k8s manifests`
