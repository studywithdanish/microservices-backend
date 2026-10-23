# Observability Phase 8: SLOs and Error Budgets

Phase 8 turns raw telemetry into explicit reliability targets. Prometheus now calculates availability and latency service-level indicators for each active application, tracks rolling 30-day error budgets, and evaluates multi-window burn-rate alerts. Grafana provides a provisioned SLO dashboard, while Alertmanager uses the existing critical and warning routes.

No public API or business workflow changes in this phase.

## Objectives

| SLO | Good event | Objective | Rolling window | Error budget |
| --- | --- | ---: | ---: | ---: |
| HTTP availability | A non-actuator request that does not return HTTP 5xx | 99.5% | 30 days | 0.5% failed requests |
| HTTP latency | A non-actuator request completed within 2 seconds | 95% | 30 days | 5% slow requests |

HTTP 4xx responses remain valid availability events because they normally represent authentication, authorization, validation, or missing-resource outcomes rather than server failure. `/actuator/**` and `UNKNOWN` routes are excluded so Prometheus scraping and unmatched-path noise do not improve or distort the user-facing SLO.

The objectives are deliberately below 100%. An error budget creates a measurable tradeoff between reliability work and feature delivery instead of treating every isolated failure as an outage.

## Error-budget model

For a target `T` and measured good-event ratio `S`, Prometheus calculates remaining budget as:

```text
remaining = clamp((S - T) / (1 - T), 0, 1)
```

A value of `1` means the entire budget remains, `0.5` means half remains, and `0` means the rolling objective has been exhausted. Burn rate is the current bad-event ratio divided by the allowed bad-event ratio:

```text
availability burn = HTTP 5xx ratio / 0.005
latency burn      = requests slower than 2s ratio / 0.05
```

A burn rate of `1` is exactly sustainable for the 30-day objective. Values greater than `1` consume the budget faster than it is replenished.

## Instrumentation boundary

All active Spring applications publish explicit `100ms`, `300ms`, `1s`, `2s`, and `5s` HTTP histogram buckets. The two-second boundary makes the latency SLI deterministic instead of relying on a backend-specific percentile approximation.

The rules live in `deploy/k8s/prometheus/blog-platform.yml` and are shared by Docker Compose, Kubernetes, local CI, and the production-style Compose deployment.

Important recorded series include:

```promql
blog:slo_http_availability:ratio30d
blog:slo_http_availability:error_budget_remaining30d
blog:slo_http_latency:ratio30d
blog:slo_http_latency:error_budget_remaining30d
blog:slo_http_errors:ratio5m
blog:slo_http_errors:ratio1h
blog:slo_http_slow:ratio5m
blog:slo_http_slow:ratio1h
```

## Multi-window burn-rate alerts

Each SLO has two alert classes:

| Alert | Windows that must both breach | Burn threshold | Severity | Intent |
| --- | --- | ---: | --- | --- |
| Fast burn | 5 minutes and 1 hour | `14.4x` | critical | Page for a severe incident consuming budget rapidly |
| Slow burn | 6 hours and 3 days | `1x` | warning | Investigate persistent degradation before the monthly objective is lost |

Both windows must breach, reducing noise from one short spike or one stale long-window average. Alerts also require a minimum request rate so an isolated failure on an idle service does not page the team.

Alertmanager keeps the existing delivery policy:

- Critical fast-burn alerts route to the on-call receiver.
- Warning slow-burn alerts route to the backend-team receiver.
- A critical alert inhibits the corresponding warning notification during the same incident.

## Grafana workflow

Open Grafana at `http://localhost:3001`, then select **Blog Platform / Blog Platform - SLO Overview**.

1. Select an application.
2. Check the 30-day availability and latency SLIs.
3. Check the remaining error budgets.
4. Compare the 5-minute, 1-hour, and 6-hour burn rates.
5. Use **Service detail** for resource, JVM, database-pool, and endpoint signals.
6. Open a related trace in Tempo to find the slow or failing span.
7. Follow its trace ID or correlation ID into the centralized Kibana logs.

New installations need traffic history before a rolling 30-day result is representative. This is expected; missing history is not treated as perfect reliability.

## Incident runbooks

### Fast burn: availability or latency

1. Acknowledge the critical alert and identify its `service` and `slo` labels.
2. Confirm both short and long burn windows on the SLO dashboard.
3. Check deployment time, request rate, 5xx ratio, p95 latency, saturation, and downstream availability.
4. Use Tempo to isolate the failing or slow span and pivot to Kibana with its trace ID.
5. If a recent release caused the regression, stop further rollout and use the service rollback procedure.
6. If the fault is downstream, protect the entry point with the existing timeout/fallback boundary and restore that dependency.
7. Record the incident start, customer impact, budget consumed, mitigation, and follow-up owner.

Do not restart every service blindly. A fast-burn alert identifies customer-impacting symptoms; traces, logs, metrics, and recent-change data identify the cause.

### Slow burn: availability or latency

1. Confirm the degradation is persistent and not only low-traffic arithmetic.
2. Break the raw HTTP metrics down by `uri`, `status`, and `outcome` in Prometheus or Grafana Explore.
3. Compare the service against the same time on previous days.
4. Inspect database, Kafka, downstream HTTP, garbage collection, CPU, and memory signals.
5. Create an owned reliability item with a deadline before the remaining budget reaches zero.
6. Raise release risk when less than 25% of the budget remains; prioritize reliability over risky feature changes when it reaches zero.

### SLO data missing

1. Check `up{job="blog-services"}` and the service `/actuator/prometheus` endpoint.
2. Confirm the `application` label and the `le="2.0"` histogram bucket exist.
3. Open Prometheus **Status / Rules** and inspect the `blog-platform-slo-recording` group.
4. Check `prometheus_rule_evaluation_failures_total` and the Prometheus logs.
5. Validate and reload the rule file only after correcting the underlying query or scrape issue.

## Local verification

Validate the Prometheus configuration and rules with the pinned image:

```powershell
docker run --rm --entrypoint promtool `
  -v "${PWD}/deploy/k8s/prometheus:/etc/prometheus:ro" `
  prom/prometheus:v3.15.0 `
  check config /etc/prometheus/prometheus.yml

docker run --rm --entrypoint promtool `
  -v "${PWD}/deploy/k8s/prometheus:/etc/prometheus:ro" `
  prom/prometheus:v3.15.0 `
  check rules /etc/prometheus/blog-platform.yml

docker run --rm --entrypoint promtool `
  -v "${PWD}/deploy/k8s/prometheus:/etc/prometheus:ro" `
  prom/prometheus:v3.15.0 `
  test rules /etc/prometheus/slo-rules.test.yml
```

Start or rebuild the relevant containers so the new histogram boundaries are active:

```powershell
docker compose up -d --build api-gateway identity-service post-service content-service notification-service prometheus alertmanager grafana
```

Generate representative traffic, wait for at least two rule evaluations, and query:

```powershell
Invoke-RestMethod "http://localhost:9096/api/v1/query?query=blog%3Aslo_http_requests%3Arate5m"
Invoke-RestMethod "http://localhost:9096/api/v1/query?query=blog%3Aslo_http_availability%3Aratio30d"
Invoke-RestMethod "http://localhost:9096/api/v1/rules"
```

Validate deployment files:

```powershell
docker compose config --quiet
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.production.example config --quiet
kubectl kustomize deploy/k8s | Out-Null
```

## Production decisions

- Objectives should be reviewed with product and operations owners after enough baseline traffic exists; they are not arbitrary uptime claims.
- Alert routing should page only for actionable, user-impacting fast burns. Slow burns normally create planned work.
- Prometheus defaults to 35-day retention so the 30-day window can be evaluated locally. A production deployment should also monitor capacity and use durable managed metrics storage or object-backed long-term storage when required.
- SLOs are evaluated per application. A later product-level SLO can use gateway journeys or synthetic probes when externally hosted production endpoints exist.
- Error-budget policy is a release decision aid, not permission to ignore known defects or security issues.

## Interview explanation

“I defined availability and latency in terms of user-visible good events, not infrastructure uptime alone. Prometheus recording rules calculate rolling 30-day SLIs and remaining error budgets per service. Multi-window burn alerts require both a short and long window, so severe failures page quickly while persistent degradation creates a warning without alerting on every spike. Grafana shows the objective, remaining budget, traffic, and burn rate; an engineer then pivots into Tempo traces and Kibana logs for root cause. The rules, dashboard, alert routing, validation, and runbook are version controlled and identical across Docker and Kubernetes.”

## Phase boundary

Phase 8 provides automated reliability measurement and budget-aware alerting. Phase 9 will complete the observability program with production hardening and repeatable incident validation: synthetic user-journey probes, controlled failure drills, backup/retention checks, and a final evidence-based operational readiness checklist.
