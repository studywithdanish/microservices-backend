# Observability Phase 9: Synthetic Monitoring and Operational Readiness

Phase 9 closes the nine-phase observability roadmap. It turns the metrics, logs, traces, alerts, and SLOs from the earlier phases into a repeatable readiness process rather than adding another isolated monitoring screen.

## What this phase adds

- A pinned Blackbox Exporter `v0.28.0` deployment for Docker Compose and Kubernetes
- Three private, read-only synthetic journeys evaluated every 30 seconds
- Prometheus recording rules for five-minute journey availability and latency
- Critical failure, sustained slowness, and probe-engine health alerts
- A provisioned **Blog Platform - Synthetic Readiness** Grafana dashboard
- Semantic Prometheus rule tests and Blackbox configuration validation in Jenkins
- A controlled Kubernetes failure drill that restores the frontend in a `finally` block
- A read-only operational-readiness verifier for workloads, storage, probes, rules, retention, and tracing
- A production evidence template for backup/restore and incident-review sign-off

## Probe design

| Journey | Internal target | Module | Purpose |
| --- | --- | --- | --- |
| `gateway-health` | `http://api-gateway:9090/actuator/health` | `spring_health` | Requires HTTP 200 and a Spring health body containing `"status":"UP"` |
| `public-post-list` | `http://api-gateway:9090/api/posts?pageNo=0&pageSize=1` | `http_2xx` | Verifies a real, public API read through the gateway |
| `frontend-home` | `http://frontend:80/` | `http_2xx` | Verifies that the React delivery path returns a successful response |

These probes do not register users, authenticate, create posts, or mutate data. The existing `smoke-test.ps1` remains the on-demand authenticated journey. This separation is deliberate: a continuous probe should be safe to run every 30 seconds, while a data-creating journey needs explicit cleanup, test identities, and a lower schedule.

Blackbox Exporter is cluster-internal and is not published through Caddy, a NodePort, or a host port. Prometheus uses the standard multi-target exporter pattern: the target and module become request parameters while the scrape address is relabelled to `blackbox-exporter:9115`.

## Metrics and alerts

Useful raw metrics:

```promql
probe_success{job="synthetic-http"}
probe_duration_seconds{job="synthetic-http"}
probe_http_status_code{job="synthetic-http"}
up{job="blackbox-exporter"}
```

Phase 9 recording rules:

```promql
blog:synthetic_probe_success:ratio5m
blog:synthetic_probe_duration:avg5m
```

Alerts:

| Alert | Hold time | Severity | Meaning |
| --- | --- | --- | --- |
| `BlogSyntheticProbeFailed` | 2 minutes | critical | A journey is continuously failing |
| `BlogSyntheticProbeSlow` | 10 minutes | warning | Five-minute average journey duration is above two seconds |
| `BlackboxExporterDown` | 2 minutes | critical | The probe engine itself is unavailable, so journey results are untrustworthy |

The separate exporter alert prevents responders from treating missing synthetic data as an application outage.

## Run locally with Docker Compose

```powershell
docker compose up -d --build
docker compose ps
```

Open Prometheus at `http://localhost:9096`, choose **Status > Target health**, and confirm:

- `blackbox-exporter` is up
- the `gateway-health` and `public-post-list` targets are up

The development Compose file belongs to the backend repository and does not start the sibling React repository, so it intentionally uses `synthetic-targets.local.yml` and omits `frontend-home`. Production Compose and Kubernetes use `synthetic-targets.yml` and check all three journeys.

Open Grafana at `http://localhost:3001` and select **Blog Platform > Blog Platform - Synthetic Readiness**.

## Run with local Kubernetes

Deploy or refresh the stack:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -Profile blog-platform
```

Open operator-only endpoints in separate terminals:

```powershell
kubectl -n blog-platform port-forward service/prometheus 19090:9090
kubectl -n blog-platform port-forward service/grafana 13001:3000
kubectl -n blog-platform port-forward service/elasticsearch 19200:9200
kubectl -n blog-platform port-forward service/tempo 13200:3200
```

Run the non-mutating readiness checks:

```powershell
.\deploy\k8s\scripts\verify-operational-readiness.ps1 -Profile blog-platform
```

The verifier fails when required workloads or persistent claims are unhealthy, any synthetic journey is failing, Prometheus recently failed to evaluate rules, 35-day metric retention is missing, 14-day log retention is missing, or Tempo is not ready.

## Controlled failure drill

The drill intentionally scales only the local `frontend` deployment to zero, waits for `BlogSyntheticProbeFailed`, and restores the original replica count even if the verification fails.

Prerequisites:

1. Use the dedicated `blog-platform` Minikube context.
2. Keep the Prometheus port-forward on `localhost:19090` running.
3. Do not run the script against a shared UAT or production cluster.

Run:

```powershell
.\deploy\k8s\scripts\test-synthetic-monitoring.ps1 `
  -Profile blog-platform `
  -AcknowledgeImpact
```

Expected evidence:

- `probe_success{journey="frontend-home"}` becomes `0`
- the alert changes from pending to firing after two minutes
- Alertmanager receives the critical alert
- the script restores the frontend and waits for rollout completion
- the alert resolves after successful probes resume

## Incident runbooks

### Synthetic probe failed

1. Check `up{job="blackbox-exporter"}`. If it is zero, follow **Blackbox Exporter down** instead of changing the application.
2. Check `probe_http_status_code`, `probe_duration_seconds`, and the failed `journey` label.
3. For `gateway-health`, inspect gateway and downstream target health in Prometheus.
4. For `public-post-list`, pivot to gateway request rate/error/latency, then use Tempo to locate the slow or failing downstream span.
5. For `frontend-home`, inspect the frontend workload, service endpoints, ingress/reverse proxy, and recent deployment.
6. Search Kibana by the failing time window, service, and correlation ID from a reproduced request.
7. Roll back the most recent relevant change when evidence points to deployment regression. Do not restart every service without evidence.

### Synthetic probe slow

1. Compare probe duration with gateway and service p95 latency.
2. Inspect Tempo for the longest span and determine whether time is spent in the gateway, a business service, MySQL, Kafka, or an external dependency.
3. Check saturation signals: JVM threads, heap, database pool, CPU throttling, pod restarts, and Kafka lag/backlog.
4. Compare the event time with deployments and SLO burn alerts.
5. Escalate only after identifying the bottleneck or a clear dependency owner.

### Blackbox Exporter down

1. Check the `blackbox-exporter` deployment, pod events, readiness probe, and mounted ConfigMap.
2. Confirm Prometheus can resolve and scrape `blackbox-exporter:9115`.
3. Review `blackbox_exporter_config_last_reload_successful` and container logs.
4. Treat other synthetic alerts as uncertain until the exporter is healthy.
5. Application metrics, traces, and logs remain available and should be used to confirm whether a customer-facing outage also exists.

## Retention and backup evidence

The repository can validate configured retention and persistent-volume wiring, but it cannot honestly prove that a cloud database snapshot or S3 backup can be restored without access to that environment. Production readiness therefore requires evidence rather than a fake green check.

Use `docs/operational-readiness-evidence.example.md` as the review template. For a real production gate:

1. Copy it outside the public repository or into the approved evidence store.
2. Record the database snapshot ID, object-storage versioning/retention policy, restore target, timestamps, RPO/RTO, result, owner, and incident ticket.
3. Pass the completed evidence path to the verifier:

```powershell
.\deploy\k8s\scripts\verify-operational-readiness.ps1 `
  -Profile blog-platform `
  -BackupEvidencePath C:\secure-evidence\blog-platform-restore-2026-10.md
```

Local portfolio runs may omit this parameter; the script clearly reports that production backup evidence was not checked.

## CI validation

Jenkins now validates:

```text
Blackbox configuration parsing
Prometheus scrape configuration
Prometheus alert and recording rules
Synthetic alert behavior through promtool test rules
Kubernetes rendering
Local and production Compose rendering
Grafana dashboard JSON
```

This validates configuration semantics without requiring the CI agent to deploy or intentionally break an environment.

## Production boundary

The included probes observe the application from inside its deployment network. They catch workload, service-routing, and application failures, but they do not prove that public DNS, TLS, CDN, WAF, or internet routing works. A production system should run an additional managed or separately hosted external probe against the public HTTPS URL. Its network and credentials must be independent from the application cluster.

Phase 9 deliberately does not expose Grafana, Prometheus, Kibana, Tempo, Alertmanager, or Blackbox Exporter in the React application. These are authenticated operator tools, not customer features.

## Completion criteria

Phase 9 is complete when:

- Compose and Kubernetes render successfully
- the Blackbox configuration and Prometheus rules pass pinned-tool validation
- all three probes are green
- the Grafana dashboard shows the journeys
- the controlled drill produces and resolves the expected alert
- the operational-readiness verifier passes
- production promotion includes current backup/restore evidence

At that point all nine observability phases are implemented. Managed external probing, managed metrics/logs/traces, and cloud backup automation are deployment-specific follow-on work, not missing application functionality.
