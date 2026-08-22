# API Gateway

The API Gateway is the public entry point for the blogging platform. Phase 3 uses the routing seam introduced in Phase 2 to send identity traffic to an independently deployed Identity Service without changing frontend API URLs.

## Responsibilities

- Route `/api/v1/auth/**` and `/api/users/**` to the Identity Service
- Route the remaining API, Swagger, and OpenAPI requests to the content backend
- Preserve `Authorization` and other request headers
- Create or validate an `X-Correlation-Id` for every routed request
- Apply browser CORS policy at the public boundary
- Return a stable JSON response when a downstream service is unavailable or times out
- Expose gateway health and info endpoints

Authentication is enforced by the Identity Service, and resource authorization is enforced by the service that owns each resource. The gateway forwards bearer tokens but does not treat forwarded identity headers as trusted authentication.

## Run Outside Docker

Start the content backend on port `9091` and the Identity Service on port `9092`:

```bash
SERVER_PORT=9091 mvn spring-boot:run
mvn -f identity-service/pom.xml spring-boot:run
```

Then start the gateway from this directory:

```bash
mvn spring-boot:run
```

The gateway listens on `http://localhost:9090` and routes to `http://localhost:9091` by default.

Configuration variables:

- `GATEWAY_PORT`
- `BACKEND_BASE_URL`
- `IDENTITY_BASE_URL`
- `CORS_ALLOWED_ORIGINS`
- `GATEWAY_TRUSTED_PROXIES`

## Tests

```bash
mvn test
```

The tests verify identity/content route separation, bearer-token forwarding, correlation IDs, gateway health, unavailable-backend responses, timeout responses, and rejection of unconfigured routes.
