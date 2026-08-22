# Microservices Backend

Spring Boot backend APIs for a blogging platform. The project is being modernized from a monolithic blog API into a production-ready backend foundation for platform engineering practice.

## Current Architecture

Phase 3 extracts identity and user management as the first independently deployable business service. Spring Cloud Gateway remains the only public API entry point, so the frontend keeps the same URLs while the gateway selects the correct downstream service.

```text
Frontend -> API Gateway :9090
              |-> Identity Service :9092 -> Identity MySQL
              `-> Content Backend :9090  -> Content MySQL
```

High-level structure:

- Spring Cloud Gateway owns the public API boundary on port `9090`
- The Identity Service owns users, credentials, roles, registration, login, and JWT issuance
- The content backend validates identity claims locally and does not query identity data
- Each business service has its own Flyway-managed MySQL database
- Both downstream services are private inside Docker and enforce their own authorization
- Controllers expose REST APIs from the current backend
- Services contain business logic
- Repositories handle persistence through Spring Data JPA
- Spring Security protects write/admin operations with JWT-based authentication
- MySQL is used for local/prod-style runtime, while tests use an isolated H2 profile
- Docker Compose runs the gateway, private backend, and MySQL for local platform testing

## Engineering Improvements

This repository is being upgraded step by step with a commit history that shows the modernization journey.

Completed improvements:

- Upgraded to Spring Boot 3 and Spring Security 6
- Replaced legacy Swagger setup with springdoc OpenAPI
- Externalized environment-specific configuration
- Removed secrets from current application configuration
- Added Dockerfile and Docker Compose runtime
- Added Jenkins CI pipeline with Maven test/package and Docker image build stages
- Added Actuator health and info endpoints
- Added Docker healthcheck using `/actuator/health`
- Added Flyway baseline migration for repeatable production schema creation
- Added service, controller, and security access tests
- Replaced console output with structured SLF4J logging
- Encoded passwords consistently across user mutation flows
- Refactored services and controllers to constructor injection
- Completed Phase 1 microservice-readiness boundaries and ownership controls
- Completed Phase 2 API Gateway routing, correlation IDs, failure handling, Docker integration, and CI coverage
- Completed Phase 3 Identity Service extraction, database ownership, gateway routing, and claim-based downstream authorization

## Phase 1: Microservice-Ready Modular Monolith

Phase 1 keeps one deployable Spring Boot application while reducing the coupling that would make later service extraction risky.

Implemented boundaries and safeguards:

- Replaced the shared user DTO with separate registration, update, and response contracts so passwords are never returned by user APIs
- Replaced cross-domain JPA object graphs with scalar ownership identifiers (`authorId` and `postId`) across user, post, and comment boundaries
- Derived the acting user from the authenticated JWT identity instead of trusting client-supplied user IDs
- Enforced owner-or-admin authorization for user, post, comment, and post-image mutations
- Restricted category mutations and user administration APIs to administrators
- Added validated post and comment request contracts and strengthened login/registration validation
- Added safe image type checks and normalized storage paths to prevent unsupported uploads and path traversal
- Added Flyway migration `V2__prepare_service_ownership_boundaries.sql` for ownership indexes, removal of the cross-boundary post/user foreign key, and comment lifecycle handling
- Added unit and security tests for the new ownership and authorization rules

The preferred authenticated post creation endpoint is:

```text
POST /api/posts
```

For a gradual frontend migration, the existing post-creation route remains available but validates its `userId` against the authenticated user. Registration passwords must be 8-72 characters.

## Phase 2: API Gateway Migration Seam

Phase 2 introduces a separately built and deployed Spring Cloud Gateway without prematurely splitting business data.

```text
Frontend -> API Gateway :9090 -> Modular backend :9090 (private Docker network) -> MySQL
```

Implemented gateway capabilities:

- Stable public routing for `/api/**`, Swagger UI, and OpenAPI endpoints
- Transparent forwarding of JWT bearer tokens to the backend
- Validated or generated `X-Correlation-Id` request and response headers
- Central browser CORS policy with duplicate downstream headers removed
- Explicit connection and response timeouts
- Consistent `503` and `504` JSON responses for unavailable or slow downstream services
- Gateway-owned health and info endpoints
- Backend isolation from the host network in Docker Compose
- Independent gateway tests, Maven build, Docker image, and Jenkins stages
- Production Caddy routing through the gateway instead of directly to the backend

Phase 2 deliberately retained authentication in the backend. Phase 3 completes the next strangler step described below.

## Phase 3: Identity Service Extraction

The first business capability now runs as a standalone Spring Boot service:

- `/api/v1/auth/**` and `/api/users/**` route to the Identity Service
- All other `/api/**` requests continue to route to the content backend
- Existing frontend URLs and JSON contracts are preserved
- The Identity Service owns a separate `blog_identity` schema and Flyway history
- Password hashing, credential checks, roles, user profiles, and JWT issuance were removed from the content backend
- JWTs carry signed `userId`, email subject, and role claims
- The content backend authorizes ownership from verified claims without a cross-service database lookup
- Docker Compose, production Compose, Jenkins, health checks, and automated tests cover the new service

The detailed rollout, data migration, rollback, and smoke-test guide is in [Phase 3 Identity Service](docs/phase-3-identity-service.md).

## Tech Stack

- Java 17
- Spring Boot 3.5
- Spring Security 6
- Spring Cloud Gateway 4.3
- Project Reactor and WebFlux
- Spring Data JPA
- MySQL
- JWT authentication
- springdoc OpenAPI
- Spring Boot Actuator
- Flyway
- Docker and Docker Compose
- Jenkins
- JUnit 5, Mockito, MockMvc, Spring Security Test

## Run Locally With Docker

Copy the environment template:

```bash
cp .env.example .env
```

Start both databases, both business services, and the gateway:

```bash
docker compose up --build
```

The gateway runs at:

```text
http://localhost:9090
```

Swagger UI is available at:

```text
http://localhost:9090/swagger-ui/index.html
```

Health and application info endpoints are available at:

```text
http://localhost:9090/actuator/health
http://localhost:9090/actuator/info
```

Check container health status:

```bash
docker compose ps
```

Stop the stack:

```bash
docker compose down
```

Remove local database and uploaded image volumes:

```bash
docker compose down -v
```

## Run Tests

```bash
mvn test
```

Tests use an isolated H2 database profile and do not require local MySQL.

Run the Identity Service tests independently:

```bash
mvn -f identity-service/pom.xml test
```

Run the gateway tests independently:

```bash
mvn -f gateway-service/pom.xml test
```

## Jenkins Pipeline

This repository includes a `Jenkinsfile` for a basic CI pipeline.

Pipeline stages:

- Checkout source code
- Verify Java and Maven versions
- Run backend, Identity Service, and gateway Maven tests
- Publish JUnit test reports
- Package all three Spring Boot applications
- Build backend, Identity Service, and gateway Docker images
- Archive all generated JAR artifacts

Expected Jenkins tool names:

- JDK: `jdk17`
- Maven: `maven3`

The Jenkins agent must also have Docker installed and permission to run Docker commands.

Docker images are tagged as:

```text
blog-app-apis:<jenkins-build-number>
blog-app-apis:latest
blog-api-gateway:<jenkins-build-number>
blog-api-gateway:latest
blog-identity-service:<jenkins-build-number>
blog-identity-service:latest
```

Create a Jenkins Pipeline job and point it to this GitHub repository. Jenkins will read the `Jenkinsfile` from the repository root.

## Environment Variables

Use `.env.example` as the reference for local development. Do not commit `.env`.

Use `.env.production.example` as the reference for live deployment. Do not commit real production values.

Important variables:

- `SPRING_PROFILES_ACTIVE`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `IDENTITY_DB_URL`
- `IDENTITY_DB_USERNAME`
- `IDENTITY_DB_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`
- `GATEWAY_PORT`
- `BACKEND_BASE_URL` (manual non-Docker gateway runs)
- `IDENTITY_BASE_URL` (manual non-Docker gateway runs)

## API Documentation

OpenAPI documentation is generated by springdoc:

```text
/v3/api-docs
/swagger-ui/index.html
```

## Operational Endpoints

The API Gateway exposes only safe public Actuator endpoints by default:

```text
/actuator/health
/actuator/info
```

These endpoints report gateway health and are used for local Docker checks, CI/CD verification, and future AWS or monitoring integrations. The backend has its own internal container healthcheck.

## Production Readiness

The platform is ready for a gateway-fronted portfolio deployment using Docker Compose.

Important deployment behavior:

- Local development uses the `dev` profile by default
- Live deployment should use `SPRING_PROFILES_ACTIVE=prod`
- Flyway creates the baseline database schema in a repeatable way
- The production profile uses `spring.jpa.hibernate.ddl-auto=validate`
- Secrets and environment-specific values are passed through environment variables
- CORS must be restricted to the deployed frontend URL
- Actuator exposes only `/actuator/health` and `/actuator/info`

For a frontend deployed at `https://your-domain.com`, use:

```text
CORS_ALLOWED_ORIGINS=https://your-domain.com
```

## Dependency And Security Checks

Backend dependency versions are managed through the Spring Boot parent wherever possible. This keeps Spring, Jackson, Tomcat, validation, logging, and test dependencies aligned with the selected Spring Boot release.

Recommended checks before deployment:

```bash
mvn test
mvn -f identity-service/pom.xml test
mvn -f gateway-service/pom.xml test
mvn dependency:tree
mvn -f identity-service/pom.xml dependency:tree
mvn -f gateway-service/pom.xml dependency:tree
```

Security-related improvements already applied:

- No committed runtime secrets in current configuration
- BCrypt password encoding
- JWT secret externalized through `JWT_SECRET`
- Stateless Spring Security filter chain
- Public endpoints explicitly whitelisted in each service
- Identity data is no longer shared with the content database
- Downstream authorization uses verified JWT identity and role claims
- Production schema managed by Flyway instead of Hibernate auto-create
- Health endpoint available for Docker, CI/CD, and AWS checks

## Deployment Roadmap

The planned deployment path is incremental and cost-aware:

1. Deploy the gateway, Identity Service, and content backend as the stable AWS baseline.
2. Store runtime configuration as environment variables.
3. Add a Docker image registry push stage in Jenkins.
4. Add Terraform for repeatable infrastructure.
5. Add centralized logs and basic metrics.
6. Add Prometheus and Grafana after the live deployment is stable.

The production Docker Compose and AWS runbook are available in:

```text
deploy/
```

## Microservices Migration Roadmap

The monolith will be split only after the production baseline is stable.

Planned service boundaries:

- API Gateway (completed in Phase 2)
- Identity/Auth/User service (completed in Phase 3)
- Post service
- Category/Comment service

Migration strategy:

1. Keep the gateway-fronted modular monolith live as the stable baseline.
2. Extract Auth/User as the first independently owned business service (completed).
3. Extract content capabilities behind the existing gateway one route group at a time.
4. Add service-to-service communication only where needed.
5. Move toward independent CI/CD pipelines per service.
6. Replace the shared HMAC key with asymmetric signing and public-key/JWKS verification.
7. Add Kubernetes after Docker, Jenkins, AWS, Terraform, and monitoring are already understood.

This avoids premature complexity and shows an incremental migration approach suitable for real production systems.
