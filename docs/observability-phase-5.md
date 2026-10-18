# Observability Phase 5: Centralized Structured Logging

Phase 5 adds a searchable application-log pipeline without changing public APIs or business behavior.

## Outcome

The five active Spring Boot applications emit Elastic Common Schema (ECS) JSON to standard output:

- API Gateway
- Identity Service
- Post Service
- Content Service
- Notification Service

Filebeat collects only workloads that explicitly opt in, decodes the ECS JSON, and sends events to daily `blog-platform-logs-*` indices in Elasticsearch. Kibana receives an idempotently provisioned **Blog Platform Logs** data view. A lifecycle policy deletes indices after 14 days so a portfolio or single-server environment does not grow forever.

```text
Spring Boot ECS JSON -> container stdout -> Filebeat -> Elasticsearch -> Kibana Discover
```

Spring Boot's built-in structured logging is used instead of an additional JSON encoder dependency. Each event includes standard fields such as `@timestamp`, `service.name`, `service.environment`, `log.level`, `log.logger`, `process`, and `message`. Stack traces are capped at 16 KiB by default.

## Versions and storage

- Elasticsearch: `9.3.1`
- Kibana: `9.3.1`
- Filebeat: `9.3.1`
- Local/production Compose storage: named `elasticsearch_data` and `filebeat_data` volumes
- Local Kubernetes storage: a 5 GiB Elasticsearch PVC and per-node Filebeat registry directory
- Retention: 14 days

The three Elastic components intentionally use the same version. The application index has one shard and zero replicas because the supplied Docker/Kubernetes environments are single-node learning environments.

## Local Docker Compose

Docker Desktop should have at least 6-8 GiB of memory available for the complete application and observability stack.

```powershell
docker compose up -d --build
docker compose ps
```

Operator interfaces:

```text
Kibana:        http://localhost:5601
Elasticsearch: http://localhost:9200
Grafana:       http://localhost:3001
Prometheus:    http://localhost:9096
Alertmanager:  http://localhost:9097
```

Generate a recent application event:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:9090/actuator/health
```

Then validate collection:

```powershell
.\deploy\k8s\scripts\test-centralized-logging.ps1 `
  -ElasticsearchUrl http://localhost:9200 `
  -KibanaUrl http://localhost:5601
```

## Kibana investigation workflow

Open Kibana, select **Discover**, and choose **Blog Platform Logs**. Useful KQL searches include:

```text
service.name : "post-service"
service.name : "post-service" and log.level : "ERROR"
service.environment : "production" and log.level : ("WARN" or "ERROR")
message : "*timed out*"
event.dataset : "blog-platform.application"
```

When Phase 6 adds correlation context to every service, this mapping is already prepared:

```text
correlationId : "the-id-returned-to-the-client"
```

A normal incident investigation is:

1. Start with the alert time, affected service, HTTP route, and severity from Grafana/Alertmanager.
2. Restrict Kibana to that time window and `service.name`.
3. Filter `log.level` to `WARN` and `ERROR`.
4. Inspect the exception type, stack trace, container/pod metadata, and adjacent events.
5. In Phase 6, pivot using the correlation ID across all services involved in the request.
6. Compare the log evidence with Prometheus latency/error metrics before deciding on rollback or repair.

## Local Kubernetes

The complete profile now needs at least 8 GiB because Elasticsearch and Kibana run alongside Kafka, MySQL, Prometheus, Grafana, Alertmanager, and the applications:

```powershell
minikube start -p blog-platform --driver=docker --memory=8192 --cpus=4
.\deploy\k8s\scripts\deploy-local.ps1 -Profile blog-platform
```

Use two temporary port-forwards:

```powershell
kubectl -n blog-platform port-forward service/kibana 15601:5601
kubectl -n blog-platform port-forward service/elasticsearch 19200:9200
```

Then run:

```powershell
.\deploy\k8s\scripts\test-centralized-logging.ps1
```

Useful diagnostics:

```powershell
kubectl -n blog-platform get pods,pvc
kubectl -n blog-platform logs daemonset/filebeat
kubectl -n blog-platform logs statefulset/elasticsearch
kubectl -n blog-platform logs deployment/kibana
kubectl -n blog-platform logs job/logging-setup
```

## Production security boundary

The committed environment is intentionally self-contained and does not contain cloud credentials or live infrastructure changes. Elasticsearch authentication is disabled in this single-node reference topology. In production Compose, Elasticsearch and Kibana bind only to `127.0.0.1` and are not routed through Caddy; operators use an authenticated SSH tunnel.

For a real multi-user production deployment, use Elastic Cloud or enable Elasticsearch TLS and authentication, store Filebeat credentials in a secret manager, apply least-privilege roles, encrypt snapshots, and place the logging cluster on a private network. Do not expose port `9200` or `5601` to the public internet.

Filebeat reads the Docker socket in the Compose topology and host container logs in Kubernetes. Those are privileged data sources and must remain limited to the logging agent. Application code must never log JWTs, cookies, passwords, database credentials, uploaded file contents, or other personal data.

## CI validation

Jenkins validates both Filebeat configurations using the pinned Filebeat image. The existing pipeline also renders Docker Compose and Kustomize, while `CentralizedLoggingConfigurationTest` validates the lifecycle policy, index template, searchable ECS fields, and presence of the complete deployment stack.

Phase 5 centralizes logs. End-to-end correlation propagation belongs to Phase 6, and distributed traces belong to Phase 7.
