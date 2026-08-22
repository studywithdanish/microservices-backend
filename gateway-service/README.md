# API Gateway

The API Gateway is the public entry point for the blogging platform. Phase 2 introduces it in front of the existing modular monolith so later services can be extracted without changing frontend API URLs.

## Responsibilities

- Route API, Swagger, and OpenAPI requests to the current backend
- Preserve `Authorization` and other request headers
- Create or validate an `X-Correlation-Id` for every routed request
- Apply browser CORS policy at the public boundary
- Return a stable JSON response when the backend is unavailable or times out
- Expose gateway health and info endpoints

Authentication and authorization remain enforced by the backend during Phase 2. The gateway does not treat forwarded identity headers as trusted authentication.

## Run Outside Docker

Start the backend on port `9091`:

```bash
SERVER_PORT=9091 mvn spring-boot:run
```

Then start the gateway from this directory:

```bash
mvn spring-boot:run
```

The gateway listens on `http://localhost:9090` and routes to `http://localhost:9091` by default.

Configuration variables:

- `GATEWAY_PORT`
- `BACKEND_BASE_URL`
- `CORS_ALLOWED_ORIGINS`
- `GATEWAY_TRUSTED_PROXIES`

## Tests

```bash
mvn test
```

The tests verify path routing, bearer-token forwarding, correlation IDs, gateway health, unavailable-backend responses, timeout responses, and rejection of unconfigured routes.
