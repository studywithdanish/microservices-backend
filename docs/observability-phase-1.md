# Observability Phase 1: Metrics Foundation

Phase 1 makes every active backend service observable without deploying a monitoring platform yet. Spring Boot Actuator and Micrometer now expose Prometheus-format runtime, HTTP, JVM, database-pool, Kafka, and application metrics from each service.

## Metrics endpoints

Each service exposes an internal scrape endpoint. The URLs below apply when the services run directly from Maven or an IDE:

| Service | Local endpoint |
| --- | --- |
| API Gateway | `http://localhost:9090/actuator/prometheus` |
| Identity Service | `http://localhost:9092/actuator/prometheus` |
| Post Service | `http://localhost:9093/actuator/prometheus` |
| Content Service | `http://localhost:9094/actuator/prometheus` |
| Notification Service | `http://localhost:9095/actuator/prometheus` |

The application, environment, and instance common tags identify the source of every series. Override the defaults with `METRICS_ENVIRONMENT` and `METRICS_INSTANCE`.

The production Caddy and Kubernetes frontend proxies continue to publish only `/actuator/health` and `/actuator/info`. Prometheus endpoints remain available on the services' internal ports and should not be exposed directly to the internet.

## Business metrics

| Metric | Type | Purpose |
| --- | --- | --- |
| `blog.auth.login.attempts` | Counter | Successful and failed sign-in attempts |
| `blog.users.registered` | Counter | Successful user registrations |
| `blog.posts.created` | Counter | Successfully created posts |
| `blog.images.operations` | Counter | Image store, load, and delete results by local or S3 provider |
| `blog.images.operation.duration` | Timer | Image operation latency by provider, operation, and outcome |
| `blog.content.changes` | Counter | Comment and category create/delete activity |
| `blog.outbox.events` | Counter | Outbox records, publications, and publish failures |
| `blog.outbox.pending` | Gauge | Current unpublished outbox backlog |
| `blog.outbox.failed.pending` | Gauge | Unpublished events with failed publish attempts |
| `blog.kafka.post.published.events` | Counter | Consumed, failed, retried, duplicate, and dead-letter Kafka events |
| `blog.notifications.created` | Counter | Notifications created from domain events |

Tags use a small fixed set of values. User IDs, post IDs, file names, exception messages, correlation IDs, and URLs are intentionally excluded to avoid unbounded Prometheus cardinality and sensitive-data leakage.

Micrometer converts dotted names to Prometheus names. For example, `blog.auth.login.attempts` is exported as `blog_auth_login_attempts_total`.

## Local verification

When running the gateway directly or through Docker Compose, verify its mapped endpoint with:

```powershell
Invoke-WebRequest http://localhost:9090/actuator/prometheus -UseBasicParsing
```

Docker Compose deliberately does not publish the four business-service ports to the host. A future Prometheus container will scrape `identity-service:9092`, `post-service:9093`, `content-service:9094`, and `notification-service:9095` over the private Compose network.

Useful checks include:

```powershell
(Invoke-WebRequest http://localhost:9090/actuator/prometheus -UseBasicParsing).Content | Select-String 'http_server_requests_seconds'
(Invoke-WebRequest http://localhost:9090/actuator/prometheus -UseBasicParsing).Content | Select-String 'jvm_memory_used_bytes'
```

When the Post Service runs directly, its outbox metric can be checked at `http://localhost:9093/actuator/prometheus` using `Select-String 'blog_outbox_pending'`.

Automated tests verify endpoint access and the values and tag sets of the custom meters.

## Next phase

Phase 2 now runs Prometheus as a collector, configures internal service scrape targets, persists time-series data, and evaluates recording and alerting rules. See [Observability Phase 2](observability-phase-2.md). Grafana dashboards remain the next phase now that collection is reliable.
