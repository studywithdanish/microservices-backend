# Observability Phase 4: Alertmanager Routing

Phase 4 turns evaluated Prometheus alerts into actionable operator notifications. Prometheus remains responsible for alert conditions; Alertmanager receives those alerts and handles grouping, deduplication, severity routing, repeat intervals, resolved notifications, inhibition, and temporary silences.

## Runtime topology

```text
Service metrics -> Prometheus rules -> Alertmanager -> operator notification
                         |                  |
                         |                  +-> local: Mailpit email inbox
                         |                  +-> production: authenticated SMTP
                         v
                   Grafana dashboards
```

The React customer application is not involved. Alertmanager, Mailpit, Prometheus, and Grafana are private operator tools.

## Routing policy

| Severity | Local recipient | Initial wait | Repeat interval | Purpose |
| --- | --- | ---: | ---: | --- |
| `critical` | `on-call@blog-platform.local` | 5 seconds | 30 minutes | Service outage, broken rules/configuration, or notification failure |
| `warning` | `backend-team@blog-platform.local` | 15 seconds | 4 hours | Elevated errors/latency, outbox backlog, publish failure, or dead-letter activity |

Alerts are grouped by alert name, team, severity, service, and application. Notifications are also sent when a group resolves.

Two inhibition policies reduce noise:

- A critical alert suppresses a warning with the same alert name and service.
- `BlogServiceDown` suppresses warning alerts for that service because downstream latency/error symptoms are secondary while the service is unavailable.

The Alertmanager UI can create temporary silences for a maintenance window. A silence should always include an owner and expiry; it is not a substitute for fixing or deleting an obsolete alert rule.

## Local Docker workflow

Start the platform:

```powershell
docker compose up -d --build
docker compose ps
```

Operator interfaces:

- Alertmanager: `http://localhost:9097`
- Local email inbox: `http://localhost:8025`
- Grafana: `http://localhost:3001`
- Prometheus: `http://localhost:9096`

Run the repeatable delivery test:

```powershell
.\deploy\k8s\scripts\test-alert-routing.ps1
```

The script submits a uniquely named critical alert, waits for the routed email in Mailpit, verifies its subject and recipient, and resolves the synthetic alert.

Mailpit is intentionally local-only. It provides real SMTP delivery and a browser inbox without sending test alerts to real people. Its messages persist in the `mailpit_data` volume. Alertmanager silences and notification state persist in `alertmanager_data`.

## Kubernetes workflow

The Minikube deployment includes internal ClusterIP services and persistent volumes for Alertmanager and Mailpit. Deploy normally, then start two temporary port-forwards:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -Profile blog-platform
kubectl -n blog-platform port-forward service/alertmanager 19093:9093
kubectl -n blog-platform port-forward service/mailpit 18025:8025
```

Run the delivery test from another terminal:

```powershell
.\deploy\k8s\scripts\test-alert-routing.ps1 `
  -AlertmanagerUrl http://localhost:19093 `
  -MailpitUrl http://localhost:18025
```

Stop both port-forwards with `Ctrl+C` afterward.

## Production SMTP configuration

Production does not deploy Mailpit. It expects an organization-controlled SMTP relay and keeps its password outside Git.

From the `deploy` directory:

```bash
cp k8s/alertmanager/alertmanager.production.example.yml \
   k8s/alertmanager/alertmanager.production.yml
mkdir -p secrets
printf '%s' 'SMTP_PASSWORD_FROM_SECRET_MANAGER' > secrets/alertmanager-smtp-password
chmod 600 secrets/alertmanager-smtp-password
```

Edit `alertmanager.production.yml` and replace:

- `alerts@example.com`
- `smtp.example.com:587`
- `backend-team@example.com`
- `on-call@example.com`

The generated configuration and `deploy/secrets/` are Git-ignored. For a real managed environment, materialize the password from AWS Secrets Manager, Vault, or the deployment platform instead of typing it into source control.

Production Compose binds Alertmanager only to server loopback. Inspect it with an SSH tunnel:

```bash
ssh -L 19093:127.0.0.1:9097 user@your-server
```

Open `http://localhost:19093`. Do not expose port `9097` in the cloud firewall.

## Configuration validation

Alertmanager 0.34.1 runs with UTF-8 strict matcher parsing. Jenkins validates both the runnable local configuration and the production example with the image's official `amtool`. Prometheus configuration/rules, Compose definitions, Kustomize output, and Grafana dashboard JSON remain part of the same pipeline.

Manual checks:

```powershell
docker run --rm --entrypoint amtool `
  -v "${PWD}\deploy\k8s\alertmanager:/etc/alertmanager:ro" `
  prom/alertmanager:v0.34.1 `
  check-config /etc/alertmanager/alertmanager.yml

docker compose config --quiet
kubectl kustomize deploy/k8s
```

## Operational runbooks

### Alertmanager down

1. Confirm the Alertmanager container/pod and its `/-/ready` endpoint.
2. Inspect Alertmanager logs for configuration or storage permission failures.
3. Confirm Prometheus can resolve `alertmanager:9093` and inspect **Status > Alertmanagers**.
4. Validate the configuration with `amtool` before restarting.
5. Use Grafana and Prometheus directly while delivery is unavailable.

### Notification delivery failure

1. Check `alertmanager_notifications_failed_total` by integration.
2. Inspect logs for SMTP authentication, TLS, DNS, timeout, or rate-limit errors.
3. Confirm the SMTP password file exists, is readable by Alertmanager, and contains no newline-related corruption.
4. Send a controlled synthetic alert after correcting the receiver.
5. Confirm both firing and resolved messages reach the intended recipient.

### Configuration reload failure

1. Run `amtool check-config` against the candidate file.
2. Check matcher quoting, receiver names, template paths, and SMTP fields.
3. Inspect `alertmanager_config_last_reload_successful_timestamp_seconds` to identify when the last valid configuration was loaded.
4. Restore the last Git-reviewed configuration and reload Alertmanager.

### Excessive or duplicate notifications

1. Inspect the alert's grouping labels and receiver in Alertmanager.
2. Check that service/application labels are stable and low-cardinality.
3. Verify `group_interval`, `repeat_interval`, and inhibition status.
4. Add a time-bounded silence only when maintenance is intentional.
5. Fix the Prometheus expression or routing rule when the alert itself is noisy.

## End-to-end validation

The Phase 4 implementation was validated using the pinned containers: a synthetic critical alert was accepted through the Alertmanager v2 API, routed to `backend-critical-email`, rendered through `blog-platform.tmpl`, and delivered through SMTP to the local Mailpit inbox.

## Phase boundary

Phase 4 delivers metrics-based alerts. It does not yet centralize service logs or provide exception search by correlation ID. That work begins in the centralized logging phase.
