# Observability Phase 3: Grafana Dashboards

Phase 3 adds an operator-facing visualization layer without exposing application metrics or the Prometheus UI to end users. Grafana reads the private Prometheus service and provisions its datasource and dashboards from version-controlled files, so Docker, Kubernetes, CI, and future environments use the same definitions.

## Runtime topology

```text
Gateway / Identity / Post / Content / Notification
                    |
                    | private /actuator/prometheus scrapes
                    v
              Prometheus :9090
                    |
                    | PromQL
                    v
                Grafana :3000
                    |
          authenticated operator access only
```

Grafana does not replace Prometheus. Prometheus collects and stores time series and evaluates rules; Grafana queries that data and presents it for investigation. Grafana is deliberately separate from the React customer application.

## Provisioned assets

| Dashboard | Purpose | Typical investigation |
| --- | --- | --- |
| **Blog Platform - Overview** | Service availability, request rate, 5xx ratio, p95 latency, heap, CPU, and database-pool pressure | Decide whether an incident is platform-wide or isolated to one service |
| **Blog Platform - Event Pipeline** | Outbox backlog/failures, Kafka publication results, DLT activity, and notification consumption | Follow a post event from database commit through Kafka to notification creation |
| **Blog Platform - Service Detail** | Templated per-service HTTP routes, latency, errors, JVM, GC, threads, and HikariCP | Drill into the service selected by the `application` variable |

The dashboard JSON is under `deploy/k8s/grafana/dashboards`. The datasource and dashboard-provider definitions are under `deploy/k8s/grafana/provisioning`. The dashboards are read-only in the UI: make durable changes in Git, review them, and redeploy.

## Local Docker usage

Copy `.env.example` to `.env`, choose a non-default `GRAFANA_ADMIN_PASSWORD`, then start the stack:

```powershell
docker compose up -d --build
docker compose ps
```

Open `http://localhost:3001`, sign in with `GRAFANA_ADMIN_USER` and `GRAFANA_ADMIN_PASSWORD`, and open the **Blog Platform** folder. Prometheus remains available locally at `http://localhost:9096` for query troubleshooting.

The `grafana_data` volume preserves users, preferences, and Grafana state. Provisioned dashboards remain Git-owned. `docker compose down` preserves both Prometheus and Grafana data; `docker compose down -v` deletes them.

## Kubernetes usage

The Kustomize deployment creates an internal `ClusterIP` service, a 2 GiB PVC, provisioning ConfigMaps, health probes, and resource limits. Deploy through the existing script, then use a temporary port-forward:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -Profile blog-platform
kubectl -n blog-platform port-forward service/grafana 13001:3000
```

Read the generated administrator password in another PowerShell window:

```powershell
$encoded = kubectl -n blog-platform get secret blog-secrets -o jsonpath="{.data.GRAFANA_ADMIN_PASSWORD}"
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded))
```

Open `http://localhost:13001`. Stop the port-forward with `Ctrl+C` when the investigation is complete.

## Production access model

Production Compose binds Grafana to the server loopback interface and does not route it through Caddy. Set a strong, unique `GRAFANA_ADMIN_PASSWORD` in `deploy/.env`, then open an authenticated SSH tunnel from the operator workstation:

```bash
ssh -L 13001:127.0.0.1:3001 user@your-server
```

Open `http://localhost:13001`. Do not open Grafana port `3001` in the cloud firewall. Anonymous access and user sign-up are disabled, analytics reporting and update checks are disabled, and the datasource cannot be edited through the UI.

For a larger organization, place Grafana behind the company's SSO-enabled internal ingress and use viewer/editor roles. That is an infrastructure concern and is intentionally outside this portfolio deployment.

## Developer incident workflow

When a UAT or production issue is reported:

1. Select the incident time range on **Platform Overview**.
2. Check whether any target is down and compare request rate, 5xx ratio, and p95 latency by service.
3. Select the affected application on **Service Detail** and identify the route, JVM, database-pool, or thread symptom.
4. For missing asynchronous notifications, use **Event Pipeline** to compare outbox, publication, DLT, and consumer metrics.
5. Copy the exact timestamp, application, route, and correlation ID from the user/API response into the centralized-log search introduced in a later observability phase.

Metrics answer *where and when* the problem happened. Logs provide the detailed exception and request context; distributed traces later show the complete call path. Keeping those roles separate prevents Grafana dashboards from becoming high-cardinality log stores.

## Configuration validation

The backend test suite parses all dashboard JSON and verifies unique dashboard and panel IDs, the Prometheus datasource UID, non-empty PromQL expressions, read-only dashboard ownership, and the expected schema version. Jenkins also renders both Compose definitions and the complete Kustomize output.

Run the same checks locally:

```powershell
mvn -Dtest=GrafanaDashboardConfigurationTest test
docker compose config --quiet
docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env.production.example config --quiet
kubectl kustomize deploy/k8s
```

After starting Grafana, verify provisioning:

```powershell
Invoke-RestMethod http://localhost:3001/api/health
```

Then confirm that the **Blog Platform** folder contains all three dashboards and that the Prometheus datasource reports success.

## Troubleshooting

- **Dashboard shows no data:** check Prometheus targets first, then confirm the dashboard time range includes recent traffic.
- **Datasource unavailable:** confirm the `prometheus` container or Kubernetes service is healthy and reachable on port `9090` from Grafana.
- **Dashboard did not update:** validate the JSON, redeploy Grafana, and inspect its logs for provisioning errors.
- **A variable has no services:** generate traffic and verify that the corresponding service exports the `application` label.
- **Login fails after a password change:** Grafana only uses the initial admin password when its database is first created. Use Grafana's administrator reset procedure or recreate only the Grafana volume in a disposable local environment.

## Phase boundary

Phase 3 provides repeatable visualization and an investigation workflow. It does not send notifications or centralize application logs. Alert routing and searchable structured logs remain later observability phases.
