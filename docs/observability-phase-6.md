# Observability Phase 6: End-to-End Correlation IDs

Phase 6 makes one safe request identifier searchable across the gateway, synchronous service calls, the transactional outbox, Kafka, and the Notification Service. It connects the centralized logs delivered in Phase 5 without changing any public business API.

## Outcome

```text
Browser or API client
  -> API Gateway
  -> Identity, Post, Content, or Notification Service
  -> private Post <-> Content HTTP call
  -> Post outbox row
  -> Kafka X-Correlation-Id header
  -> Notification consumer
  -> ECS JSON logs -> Filebeat -> Elasticsearch -> Kibana
```

Every active application uses:

- HTTP header: `X-Correlation-Id`
- SLF4J MDC field: `correlationId`
- Kafka header: `X-Correlation-Id`
- Maximum accepted length: 128 characters
- Accepted characters: letters, digits, `.`, `_`, and `-`

The gateway keeps a valid client value or generates a UUID, forwards it, exposes it to browser code through CORS, and returns it on the response. Every MVC service repeats that protection so direct private traffic and tests are also correlated. Unsafe, blank, or oversized input is replaced rather than copied into logs.

The ID is an operational identifier, not authentication, authorization, a secret, or a place for personal data.

## Synchronous propagation

The gateway writes the ID into Reactor Context. Automatic Reactor context propagation restores the matching MDC value when reactive execution changes threads, so gateway completion and failure logs contain the same `correlationId` as downstream services.

Servlet filters in Identity, Post, Content, and Notification place the ID in MDC for the request lifecycle, return it on the response, log application request completion, and always remove it afterward to prevent thread-pool leakage.

The two private reference calls also forward the header:

- Post Service -> Content Service for category validation
- Content Service -> Post Service for post validation

## Asynchronous propagation

An HTTP thread ends before the scheduled outbox publisher runs. Relying only on MDC would therefore lose the context. Post Service persists `correlation_id` with each outbox row in the same transaction as the post, then publishes that value as a Kafka record header.

Notification Service reads and validates the Kafka header before deserializing or processing the event. It restores the value into MDC for the complete consumer operation and generates a fallback UUID for older or external messages without a valid header. Existing databases are upgraded by Flyway migration `V3__add_correlation_id_to_post_outbox.sql`; historical pending rows are backfilled from their event IDs.

## Verify HTTP correlation

Start the stack and send a value through the gateway:

```powershell
$correlationId = "manual-" + [guid]::NewGuid().ToString()
$response = Invoke-WebRequest -UseBasicParsing `
  -Uri "http://localhost:9090/api/posts?pageNo=0&pageSize=5" `
  -Headers @{ "X-Correlation-Id" = $correlationId }

$response.StatusCode
$response.Headers["X-Correlation-Id"]
```

The returned value must equal `$correlationId`. An invalid value such as `contains spaces` must be replaced with a UUID.

Open Kibana at `http://localhost:5601`, select **Discover** and **Blog Platform Logs**, then search:

```text
correlationId : "manual-your-id"
```

Useful refinements are:

```text
correlationId : "manual-your-id" and service.name : "post-service"
correlationId : "manual-your-id" and log.level : ("WARN" or "ERROR")
```

For the asynchronous path, create a post while supplying `X-Correlation-Id`, wait for outbox publication, and search the same value. Post Service publication and Notification Service consumption logs should share it even though they run on different threads and at different times.

## Incident workflow

1. Copy `X-Correlation-Id` from the failed API response or support report.
2. Search that exact value in Kibana across all services.
3. Order results by time to reconstruct gateway, owner-service, private HTTP, outbox, and consumer activity.
4. Inspect WARN/ERROR entries and stack traces, then compare the same time window in Grafana.
5. Use Kafka/outbox metrics to decide whether the failure was synchronous, waiting to publish, retried, or moved to the DLT.

## Correlation ID versus trace ID

A correlation ID is deliberately simple and can be supplied to support staff or searched manually. It does not record individual spans, parent/child relationships, or precise time spent at each hop. Phase 7 adds distributed tracing for that deeper topology and latency analysis while retaining the correlation ID as a practical support handle.

## Validation

Automated tests verify gateway Reactor/MDC propagation, filter validation and cleanup in all four MVC services, both private HTTP clients, outbox persistence, Kafka producer headers, Kafka consumer restoration, and missing-header fallback. The root architecture test also protects the cross-module contract from accidental removal.
