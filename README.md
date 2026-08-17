# Microservices Backend

Spring Boot backend APIs for a blogging platform. The project is being modernized from a monolithic blog API into a production-ready backend foundation for platform engineering practice.

## Current Architecture

The current backend is in the transitional gateway phase of an incremental microservices migration. Spring Cloud Gateway is the public API entry point, while the Phase 1 modular monolith remains the private downstream application for blog, category, comment, user, and authentication APIs.

This is intentional: routing is separated before business capabilities are extracted, so the frontend can keep one stable base URL while individual API groups move to new services later.

High-level structure:

- Spring Cloud Gateway owns the public API boundary on port `9090`
- The backend is private inside Docker and continues to enforce JWT authorization
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

Authentication and resource authorization intentionally remain in the backend during this phase. The next extraction can route only Auth/User endpoints to an Identity Service while every other route continues to use the current backend.

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

Start MySQL, the backend, and the gateway:

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

Run the gateway tests independently:

```bash
mvn -f gateway-service/pom.xml test
```

## Jenkins Pipeline

This repository includes a `Jenkinsfile` for a basic CI pipeline.

Pipeline stages:

- Checkout source code
- Verify Java and Maven versions
- Run backend and gateway Maven tests
- Publish JUnit test reports
- Package both Spring Boot applications
- Build backend and gateway Docker images
- Archive both generated JAR artifacts

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
- `JWT_SECRET`
- `JWT_EXPIRATION_MS`
- `CORS_ALLOWED_ORIGINS`
- `GATEWAY_PORT`
- `BACKEND_BASE_URL` (manual non-Docker gateway runs)

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
mvn -f gateway-service/pom.xml test
mvn dependency:tree
mvn -f gateway-service/pom.xml dependency:tree
```

Security-related improvements already applied:

- No committed runtime secrets in current configuration
- BCrypt password encoding
- JWT secret externalized through `JWT_SECRET`
- Stateless Spring Security filter chain
- Public endpoints explicitly whitelisted
- Production schema managed by Flyway instead of Hibernate auto-create
- Health endpoint available for Docker, CI/CD, and AWS checks

## Deployment Roadmap

The planned deployment path is incremental and cost-aware:

1. Deploy the gateway-fronted modular monolith as the stable AWS baseline.
2. Store runtime configuration as environment variables.
3. Add a Docker image registry push stage in Jenkins.
4. Deploy the backend container on a low-cost AWS option.
5. Add Terraform for repeatable infrastructure.
6. Add centralized logs and basic metrics.
7. Add Prometheus and Grafana after the live deployment is stable.

The production Docker Compose and AWS runbook are available in:

```text
deploy/
```

## Microservices Migration Roadmap

The monolith will be split only after the production baseline is stable.

Planned service boundaries:

- API Gateway (completed in Phase 2)
- Auth/User service
- Post service
- Category/Comment service

Migration strategy:

1. Keep the gateway-fronted modular monolith live as the stable baseline.
2. Extract Auth/User as the first independently owned business service.
3. Route each extracted API group through the existing gateway.
4. Containerize each extracted service independently.
5. Add service-to-service communication only where needed.
6. Move toward independent CI/CD pipelines per service.
7. Add Kubernetes after Docker, Jenkins, AWS, Terraform, and monitoring are already understood.

This avoids premature complexity and shows an incremental migration approach suitable for real production systems.
