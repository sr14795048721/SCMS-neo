# Observability Notes (Stage 3)

- Services expose metrics via `/actuator/prometheus`.
- Minimal Prometheus scrape config is in `deploy/monitoring/prometheus.yml`.
- Recommended dashboards:
  - JVM overview (heap, GC, threads)
  - HTTP latency and error ratio per endpoint
  - Registration throughput and conflict count

Future:
- Add Loki for log aggregation.
- Add OpenTelemetry exporter for traces.
- Reference configs:
  - `deploy/monitoring/loki-config.yaml`
  - `deploy/monitoring/otel-collector-config.yaml`
