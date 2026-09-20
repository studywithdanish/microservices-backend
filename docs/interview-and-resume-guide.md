# Interview and Resume Guide

## 30-second project introduction

“I migrated a Spring Boot blogging monolith to gateway-fronted microservices using the strangler pattern. I introduced independently owned Identity, Post, Content, and Notification services, then added Kafka through a `PostPublished` workflow. Post Service uses a transactional outbox, and Notification Service consumes events idempotently with retry and dead-letter handling. JWT authorization, database-per-service ownership, Docker, Kubernetes, Jenkins, Flyway, and automated tests make the system operable.”

## 90-second explanation

“The starting point was a working monolith, so I avoided a big-bang rewrite. Phase 1 made the monolith extraction-ready by replacing cross-domain JPA object graphs with scalar IDs, separating request/response contracts, preventing password exposure, and enforcing owner-or-admin security. Phase 2 put Spring Cloud Gateway in front, adding stable routing, CORS, correlation IDs, timeouts, and consistent failure responses.

“I extracted Identity first because credentials and authorization have a clear boundary. It owns users, roles, BCrypt hashes, and JWT issuance. JWTs carry user ID and role claims, so downstream services verify identity locally without making Identity a synchronous dependency. Next, I extracted Posts and images. Posts use category snapshots for efficient reads and call Content only when a new category reference must be validated. Finally, I extracted Categories and Comments. Comments store scalar post and author IDs and validate posts through a private internal endpoint.

“Each business service owns its database and Flyway history. Only the gateway is public in Docker. The old backend is absent from the active runtime but retained temporarily for rollback. The result demonstrates migration strategy, security, data ownership, failure handling, testing, and operational readiness—not merely splitting packages into separate applications.”

## Resume bullets

Use two or three of these, depending on available space:

- Migrated a Spring Boot blogging monolith to API Gateway, Identity, Post, Content, and Notification services using the strangler pattern while preserving existing frontend API contracts.
- Established database-per-service ownership across four MySQL schemas; replaced cross-database JPA relationships with scalar identifiers, category snapshots, explicit service contracts, and asynchronous events.
- Implemented Spring Security 6, BCrypt, JWT claim-based owner/admin authorization, gateway CORS, correlation IDs, timeouts, and consistent downstream failure responses.
- Containerized the gateway, four business services, Kafka, and four databases with Docker Compose; added Flyway migrations, Actuator health checks, Jenkins build stages, and automated backend/frontend tests.
- Implemented an Apache Kafka `PostPublished` workflow using a transactional outbox, versioned events, idempotent consumption, retry handling, and a dead-letter topic.
- Added a JWT-protected Notification Service with its own MySQL database and integrated it across the gateway, Docker, Kubernetes, Jenkins, and end-to-end smoke tests.
- Integrated a React client through the gateway for registration, login, profile retrieval, post creation, and comments, proving the end-to-end service flow locally.

Do not claim AWS deployment until it has actually been completed and verified.

## Suggested project heading

**Microservices Blogging Platform — Java 17, Spring Boot 3, Kafka, Spring Cloud Gateway, Spring Security, JWT, MySQL, Flyway, Docker, Kubernetes, Jenkins, React**

GitHub description:

> Incremental monolith-to-microservices migration with Kafka, transactional outbox, idempotent consumers, Spring Cloud Gateway, database-per-service ownership, JWT, Docker, Kubernetes, Jenkins, and automated tests.

## Design questions and strong answers

### Why not rewrite the monolith in one step?

A big-bang rewrite increases delivery and rollback risk. The gateway created a migration seam, so one route group and one data boundary could move at a time while existing client URLs remained stable.

### Why extract Identity first?

Identity has a cohesive responsibility and clear data ownership. Moving it first established the JWT trust model used by later services and removed credentials from the content database.

### How do services authorize users without sharing the user database?

Identity puts immutable `userId` and role claims in a signed JWT. Post and Content validate signature, issuer, expiry, and claims locally, then enforce owner-or-admin rules against scalar owner IDs.

### Why not call Identity for every request?

That would add latency and make Identity a runtime dependency for all authenticated traffic. Local token verification keeps services available when Identity is temporarily unavailable after a token has already been issued.

### How do services maintain referential integrity without foreign keys across databases?

They validate references through explicit private APIs at write time and store scalar IDs. The Content Service validates a post before creating a comment; the Post Service validates a category before creating a post.

### Why store a category snapshot in a post?

It preserves the existing nested response and avoids an N+1 network call pattern on post lists. The tradeoff is eventual consistency after category changes; an event-driven refresh is a future enhancement.

### What happens when a downstream service is unavailable?

Gateway timeouts return consistent `503`/`504` responses. Cross-service write validation fails closed, preventing records with unverified references. Existing independently owned reads continue where their service is healthy.

### How is the internal API protected?

Private endpoints are not routed by the public gateway and require a separate `X-Internal-Service-Token`. The credential differs from the JWT signing secret and is compared safely.

### Why use one Content Service for categories and comments?

For this project size, each is too small to justify separate operational overhead. They form a pragmatic content-support boundary and can be split later if scale, team ownership, or release cadence requires it.

### What would you change for a larger production system?

Use asymmetric JWT signing and JWKS, a secrets manager, centralized logs/traces/metrics, resilience policies backed by service-level objectives, an event broker for snapshot updates, independent delivery pipelines, and infrastructure as code. Kubernetes would be introduced only when the scale and operational team justify it.

### Where are distributed transactions?

There is no cross-database ACID transaction. Each service commits only its data. For post publication, Post Service commits the post and outbox row locally, then Kafka delivers the event at least once. Notification Service uses the stable event ID as an idempotency key. Larger multi-step workflows would extend this with saga compensation.

### Why Kafka and not another REST call?

The post response should not depend on notification availability, and future consumers such as search or analytics should not increase Post Service coupling. Kafka provides durable fan-out and replay. The outbox closes the database/message dual-write gap, while consumer idempotency handles redelivery.

### Is the Kafka workflow exactly once?

No. It intentionally uses at-least-once delivery. A crash after sending but before marking an outbox row can cause a duplicate. The event ID is uniquely stored by Notification Service, so duplicate delivery does not create duplicate business state.

### What proves this is more than multiple folders?

Each active service has a separate application, image, database/schema, Flyway history, security boundary, health check, tests, and explicit route ownership. The old monolith is not in the active runtime.

## Demonstration order

1. Show the architecture diagram and explain gateway-only public access.
2. Run `docker compose ps` and gateway health.
3. Register and log in through the React client.
4. Create a post and show the corresponding notification arriving asynchronously through Kafka.
5. Create a comment to demonstrate synchronous reference validation.
6. Show the outbox, topic, idempotency constraint, and DLT configuration.
7. Show the separate Flyway migrations and databases.
8. Show automated tests and Jenkins stages.
9. Close with tradeoffs and the deliberately deferred AWS/observability roadmap.

## Be ready to explain personally

A recruiter may ask for changes live. Be able to trace one request from React to the gateway route, service controller, security filter, service layer, repository, and owned database. Also be able to explain why each boundary and tradeoff was selected; the repository should support your explanation, not replace it.
