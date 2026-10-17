# Observability Phase 2: Prometheus Collection

Phase 2 turns the metrics endpoints from Phase 1 into a working monitoring layer. A pinned Prometheus server scrapes every active backend service, evaluates reusable recording and alerting rules, and persists time-series data across container or pod restarts.

## Architecture

```text
API Gateway :9090 -----------\
Identity Service :9092 -------\
Post Service :9093 ------------> Prometheus :9090 -> persistent TSDB
Content Service :9094 --------/          |
Notification Service :9095 --/           +-> recording and alert rules
```

Prometheus reaches `/actuator/prometheus` over the private Docker or Kubernetes network. The production reverse proxy and Kubernetes frontend do not publish this endpoint or the Prometheus UI to the internet.

## Collection policy

- Scrape interval: 15 seconds
- Scrape timeout: 10 seconds
- Rule evaluation interval: 15 seconds
- Local UI port: `9096`
- Default retention: 15 days, configurable with `PROMETHEUS_RETENTION`
- Persistent storage: Docker named volume or Kubernetes `5Gi` persistent-volume claim
- Image: `prom/prometheus:v3.15.0`, pinned for repeatable builds

HTTP server histogram buckets are enabled in all five services so Prometheus can calculate a real p95 request latency. Common `application`, `environment`, and `instance` tags remain attached to application metrics.

## Run with Docker Compose

Start the platform normally:

```powershell
docker compose up --build -d
```

Open Prometheus at `http://localhost:9096` and check **Status > Target health**. The expected targets are Prometheus itself plus API Gateway, Identity, Post, Content, and Notification.

Useful command-line checks:

```powershell
Invoke-RestMethod http://localhost:9096/-/ready
(Invoke-RestMethod http://localhost:9096/api/v1/targets).data.activeTargets |
  Select-Object -ExpandProperty labels
(Invoke-RestMethod 'http://localhost:9096/api/v1/query?query=up%7Bjob%3D%22blog-services%22%7D').data.result
```

Prometheus waits for the gateway health check, then continuously retries any service that is temporarily unavailable. Its `prometheus_data` volume survives `docker compose down`; use `docker compose down -v` only when the local monitoring history should be deleted.

The production Compose definition binds Prometheus only to the server loopback interface. An operator can inspect it through an authenticated SSH tunnel rather than adding a public Caddy route:

```powershell
ssh -L 19090:127.0.0.1:9096 user@your-server
```

Then open `http://localhost:19090` on the operator workstation.

## Run with Kubernetes

Deploy through the existing Kustomize workflow. The Prometheus resources are included automatically:

```powershell
kubectl apply -k deploy/k8s
kubectl -n blog-platform rollout status deployment/prometheus
kubectl -n blog-platform get pods,service,pvc -l app=prometheus
```

Use a temporary local port-forward for operator access:

```powershell
kubectl -n blog-platform port-forward service/prometheus 19090:9090
```

Then open `http://localhost:19090`. Closing the command removes access; the ClusterIP service remains private.

## Recording rules

| Rule | Meaning |
| --- | --- |
| `blog:http_requests:rate5m` | Per-service request rate over five minutes |
| `blog:http_server_errors:rate5m` | Per-service HTTP 5xx rate |
| `blog:http_server_errors:ratio5m` | HTTP 5xx requests divided by all requests |
| `blog:http_server_latency:p95_5m` | Per-service p95 server latency over five minutes |
| `blog:outbox_pending:max` | Current Post Service outbox backlog |

Example PromQL queries:

```promql
up{job="blog-services"}
blog:http_requests:rate5m
blog:http_server_errors:ratio5m
blog:http_server_latency:p95_5m
blog:outbox_pending:max
sum by (application, outcome) (rate(blog_kafka_post_published_events_total[5m]))
```

## Alert rules and first-response runbook

Phase 2 evaluates alerts inside Prometheus. Sending notifications to email, Slack, or PagerDuty belongs to the later Alertmanager phase.

### Service down

`BlogServiceDown` fires when a backend target cannot be scraped for two minutes.

1. Open **Status > Target health** and note the target and scrape error.
2. Check the container or pod status and its `/actuator/health` endpoint.
3. Inspect the service logs using the same time window and correlation ID from the failing request.
4. Check database, Kafka, and downstream service availability before restarting anything.

### High HTTP 5xx ratio

`BlogHighHttp5xxRatio` fires when a service has traffic and more than 5% of requests return 5xx for ten minutes.

1. Compare the affected `application` with request rate and p95 latency.
2. Break down `http_server_requests_seconds_count` by `uri`, `method`, and `status`.
3. Inspect logs for the same service and time range.
4. Verify recent deployments and downstream dependency health.

### High HTTP p95 latency

`BlogHighHttpP95Latency` fires when p95 latency exceeds two seconds for ten minutes.

1. Identify the affected application and URI.
2. Compare JVM, Hikari connection-pool, and Kafka metrics.
3. Check database query time, downstream HTTP latency, CPU throttling, and memory pressure.

### Outbox backlog

`BlogOutboxBacklog` fires when more than 50 Post Service events remain unpublished for ten minutes.

1. Check Kafka broker health and Post Service connectivity.
2. Compare `blog_outbox_events_total` outcomes.
3. Inspect Post Service logs for publisher errors; do not manually delete outbox records.

### Event publishing failure

`BlogOutboxPublishFailure` reports outbox publication failures. `BlogKafkaDeadLetterEvent` reports events that exhausted Notification Service retries.

1. Inspect producer or consumer logs and Kafka availability.
2. Check whether the event payload version is supported.
3. Verify the notification database and unique event-ID constraint.
4. Replay dead-letter events only after correcting the cause and confirming idempotency.

### Prometheus rule failure

`PrometheusRuleEvaluationFailure` indicates an invalid or failing query. Run `promtool check config` and `promtool check rules`, then inspect Prometheus logs before reloading configuration.

## Configuration validation

The following commands validate the deployment and Prometheus configuration:

```powershell
docker compose config --quiet
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.production.example config --quiet
kubectl kustomize deploy/k8s | Out-Null
docker run --rm --entrypoint promtool `
  -v "${PWD}/deploy/k8s/prometheus:/etc/prometheus:ro" `
  prom/prometheus:v3.15.0 check config /etc/prometheus/prometheus.yml
docker run --rm --entrypoint promtool `
  -v "${PWD}/deploy/k8s/prometheus:/etc/prometheus:ro" `
  prom/prometheus:v3.15.0 check rules /etc/prometheus/blog-platform.yml
```

## Phase boundary

This phase collects, stores, queries, and evaluates metrics. The next observability phase adds Grafana provisioning and interview-ready service, JVM, Kafka, and business dashboards. Alertmanager routing and centralized logs follow after the dashboards are stable.
