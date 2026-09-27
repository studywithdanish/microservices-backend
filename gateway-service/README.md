# API Gateway

The API Gateway is the public entry point for the blogging platform. Phase 5 completes the strangler migration by routing every business API to an independently owned service without changing frontend URLs.

## Responsibilities

- Route `/api/v1/auth/**` and `/api/users/**` to the Identity Service
- Route post, search, user-post, category-post, and image paths to the Post Service
- Route `/api/categories/**`, `/api/posts/{postId}/comments`, and `/api/comments/**` to the Content Service
- Reject unknown and private routes instead of forwarding them to a catch-all backend
- Translate the browser's authentication cookie into an internal bearer header and remove the raw cookie
- Preserve explicit `Authorization` and other request headers for non-browser clients
- Create or validate an `X-Correlation-Id` for every routed request
- Apply browser CORS policy at the public boundary
- Return a stable JSON response when a downstream service is unavailable or times out
- Expose gateway health and info endpoints

Authentication is enforced by the Identity Service, and resource authorization is enforced by the service that owns each resource. For browsers, the gateway converts the `HttpOnly` JWT cookie to the bearer contract used internally. Explicit bearer headers remain supported for API clients, but forwarded identity headers are never treated as trusted authentication.

## Run Outside Docker

Start Identity Service on port `9092`, Post Service on port `9093`, and Content Service on port `9094`:

```bash
mvn -f identity-service/pom.xml spring-boot:run
mvn -f post-service/pom.xml spring-boot:run
mvn -f content-service/pom.xml spring-boot:run
```

Then start the gateway from this directory:

```bash
mvn spring-boot:run
```

The gateway listens on `http://localhost:9090`.

Configuration variables:

- `GATEWAY_PORT`
- `IDENTITY_BASE_URL`
- `POST_SERVICE_BASE_URL`
- `CONTENT_SERVICE_BASE_URL`
- `CORS_ALLOWED_ORIGINS`
- `AUTH_COOKIE_NAME`
- `GATEWAY_TRUSTED_PROXIES`

## Tests

```bash
mvn test
```

The tests verify identity/post/content route separation, secure cookie translation and removal, bearer-token forwarding, correlation IDs, gateway health, unavailable-service responses, timeout responses, and rejection of unconfigured routes.
