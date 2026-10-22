# Local Kubernetes Runbook

This directory deploys the complete platform to a dedicated local Minikube profile. It keeps the frontend and API Gateway public while the Identity, Post, Content, MySQL, Kafka, Prometheus, Grafana, Tempo, OpenTelemetry Collector, Alertmanager, Elasticsearch, Kibana, Filebeat, and local Mailpit inbox remain cluster-internal.

## Prerequisites

- Docker Desktop
- `kubectl`
- Minikube
- PowerShell 7+
- At least 8 GiB available to the Minikube profile

The default profile name is `blog-platform`. A dedicated profile avoids overwriting or resizing an existing Minikube environment.

## Create the cluster

```powershell
minikube start -p blog-platform --driver=docker --memory=8192 --cpus=4
```

Minikube makes `blog-platform` the current `kubectl` context. Confirm it before deployment:

```powershell
kubectl config current-context
```

## Build and deploy

From the backend repository root:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -Profile blog-platform
```

The script performs these steps:

1. Builds the five active Spring Boot service images and the React/Nginx image.
2. Loads all images into the selected Minikube profile.
3. Generates a Kubernetes Secret only when one does not already exist.
4. Deploys four MySQL StatefulSets, Kafka, Prometheus, Grafana, Tempo, OpenTelemetry Collector, Alertmanager, Elasticsearch, Kibana, Filebeat, and Mailpit with persistent state where required.
5. Waits for the databases and Kafka before deploying the services, gateway, and frontend.
6. Waits until every application Deployment is available.

To reuse images that are already loaded:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -SkipImageBuild -Profile blog-platform
```

`secret.example.yaml` documents the required secret keys, but is deliberately excluded from Kustomize and must never contain real credentials.

## Access and verify

With the Docker driver on Windows, use a port-forward in one terminal:

```powershell
kubectl -n blog-platform port-forward service/frontend 18080:80
```

Then open `http://localhost:18080` or run the full cross-service smoke test from another terminal:

```powershell
.\deploy\k8s\scripts\smoke-test.ps1 -BaseUrl http://localhost:18080 -Profile blog-platform
```

The smoke test verifies gateway health, registration, login, the authenticated profile, categories, post creation, and comment creation.

Prometheus is internal by default. To inspect targets, rules, and queries, start a separate temporary port-forward and open `http://localhost:19090`:

```powershell
kubectl -n blog-platform port-forward service/prometheus 19090:9090
```

Grafana is also internal. Start a temporary port-forward and open `http://localhost:13001`:

```powershell
kubectl -n blog-platform port-forward service/grafana 13001:3000
```

Read the generated administrator password in another PowerShell window:

```powershell
$encoded = kubectl -n blog-platform get secret blog-secrets -o jsonpath="{.data.GRAFANA_ADMIN_PASSWORD}"
[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded))
```

Sign in with user `admin`. The **Blog Platform** folder contains the overview, event-pipeline, service-detail, and SLO/error-budget dashboards.

The SLO dashboard evaluates 99.5% availability and 95% of requests under two seconds over a rolling 30-day window. See `docs/observability-phase-8.md` for burn-rate alerts and the incident workflow.

Tempo is internal. Grafana already queries it, but direct diagnostics can use a temporary port-forward:

```powershell
kubectl -n blog-platform port-forward service/tempo 13200:3200
```

Open `http://localhost:13200/ready`, or use Grafana **Explore** with the provisioned **Tempo** data source. See `docs/observability-phase-7.md` for TraceQL searches and trace-to-log investigation.

To inspect Alertmanager and the local email inbox, start two more port-forwards:

```powershell
kubectl -n blog-platform port-forward service/alertmanager 19093:9093
kubectl -n blog-platform port-forward service/mailpit 18025:8025
```

Open `http://localhost:19093` for alerts/silences and `http://localhost:18025` for routed emails. With both forwards active, verify critical routing from another terminal:

```powershell
.\deploy\k8s\scripts\test-alert-routing.ps1 `
  -AlertmanagerUrl http://localhost:19093 `
  -MailpitUrl http://localhost:18025
```

To investigate centralized application logs, start two additional port-forwards:

```powershell
kubectl -n blog-platform port-forward service/kibana 15601:5601
kubectl -n blog-platform port-forward service/elasticsearch 19200:9200
```

Open `http://localhost:15601`, select **Discover**, and choose **Blog Platform Logs**. Validate recent gateway collection with:

```powershell
.\deploy\k8s\scripts\test-centralized-logging.ps1
```

Useful diagnostics:

```powershell
kubectl -n blog-platform get pods,services,persistentvolumeclaims
kubectl -n blog-platform logs deployment/api-gateway
kubectl -n blog-platform logs deployment/identity-service
kubectl -n blog-platform logs deployment/post-service
kubectl -n blog-platform logs deployment/content-service
kubectl -n blog-platform logs deployment/notification-service
kubectl -n blog-platform logs deployment/prometheus
kubectl -n blog-platform logs deployment/grafana
kubectl -n blog-platform logs deployment/alertmanager
kubectl -n blog-platform logs deployment/mailpit
kubectl -n blog-platform logs statefulset/elasticsearch
kubectl -n blog-platform logs deployment/kibana
kubectl -n blog-platform logs daemonset/filebeat
kubectl -n blog-platform logs job/logging-setup
kubectl -n blog-platform logs statefulset/kafka
```

## Validate manifests without deploying

```powershell
kubectl kustomize deploy/k8s
```

This renders and validates the Kustomize structure without requiring a live cluster or kubeconfig. The backend Jenkins pipeline runs this check along with `docker compose config --quiet`.

When a cluster is available, add a client-side apply check:

```powershell
kubectl apply --dry-run=client --validate=false -k deploy/k8s
```

## Stop or remove the environment

Stop the profile while preserving its volumes:

```powershell
minikube stop -p blog-platform
```

Deleting the profile permanently removes its local Kubernetes workloads and data:

```powershell
minikube delete -p blog-platform
```
