# Phase 3: Identity Service Extraction

## Goal

Extract authentication and user management from the content backend without changing the frontend contract. This is the first business-service extraction in the strangler migration.

## Runtime Boundary

```text
Browser
  |
  v
API Gateway :9090
  |-- /api/v1/auth/**, /api/users/** -> Identity Service :9092 -> Identity MySQL
  `-- remaining /api/**              -> Content Backend :9090 -> Content MySQL
```

Only the gateway publishes a host port. Both business services and both databases remain private on the Docker network.

## Service Ownership

The Identity Service owns:

- registration and login
- user profiles and administration
- BCrypt password hashes
- roles and role membership
- JWT creation
- the `blog_identity` schema and its Flyway history

The content backend owns posts, categories, comments, and images. It stores scalar `authorId` values, not cross-database user entities or foreign keys.

## Authentication Contract

The Identity Service signs JWTs containing:

- `iss`: `blog-identity-service`
- `sub`: normalized user email
- `userId`: immutable numeric ownership identifier
- `roles`: authorities such as `ROLE_NORMAL` or `ROLE_ADMIN`
- standard issue and expiry times

The content backend verifies the signature, issuer, expiry, and required claims locally. It then applies owner-or-admin rules from those claims. It never calls the Identity Service or queries identity tables during a content request, avoiding a synchronous runtime dependency.

Phase 3 deliberately retains a shared HMAC secret so the new Identity Service can issue tokens that the content backend verifies during this incremental step. Tokens issued by the former monolith do not contain the new issuer and ownership claims, so users must log in again at cutover. A later security-hardening phase should move the signing key into the Identity Service only and let consumers verify an asymmetric public key or JWKS.

## Database Transition

The original content schema keeps its legacy user and role tables for rollback safety, but the Phase 3 application no longer maps or reads them. Do not delete those tables in the same release as the extraction.

For a new local environment, create users through `/api/v1/auth/register`.

For an existing environment containing real users, migrate before switching gateway routes:

1. Back up both databases.
2. Export legacy users, roles, and user-role mappings from the content database.
3. Import them into `identity_users`, `identity_roles`, and `identity_user_roles`, preserving every user ID and BCrypt hash.
4. Set the `identity_users` auto-increment value above the highest imported ID.
5. Compare source/destination row counts and sample role assignments.
6. Deploy the Identity Service with the same `JWT_SECRET` currently trusted by the content backend.
7. Test login and an ownership-protected content mutation through the gateway.
8. Switch the identity routes at the gateway.
9. Require existing browser sessions to log in again so they receive a Phase 3 token.

Preserving IDs is essential because existing posts and comments use those scalar IDs for ownership.

## Local Verification

```bash
mvn test
mvn -f identity-service/pom.xml test
mvn -f gateway-service/pom.xml test
docker compose config
docker compose up --build
```

Register and capture a token through the gateway:

```bash
curl -i -X POST http://localhost:9090/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Danish","email":"danish@example.com","password":"Password123!","about":"Platform engineer"}'

curl -s -X POST http://localhost:9090/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"danish@example.com","password":"Password123!"}'
```

Use the returned token against both services through the same public gateway. The post example assumes category `1` already exists:

```bash
curl -i http://localhost:9090/api/v1/auth/me \
  -H "Authorization: Bearer TOKEN"

curl -i -X POST http://localhost:9090/api/posts \
  -H "Authorization: Bearer TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title":"Phase 3","content":"Identity is now independent","categoryId":1}'
```

Expected behavior:

- registration returns `201`
- login returns `200` and a token
- `/api/v1/auth/me` returns the current user
- public content reads still work without a token
- protected content writes accept the Identity Service token
- a user cannot update another user's content

## Rollback

If the Identity Service deployment fails:

1. Route `/api/v1/auth/**` and `/api/users/**` back to the previous backend image.
2. Redeploy the previous content backend image, which still understands the old user schema.
3. Keep both database backups; do not merge independently changed identity records automatically.
4. Diagnose, reconcile any registrations created during the Phase 3 window, and retry the cutover.

The retained legacy tables make application rollback possible. Once Phase 3 has been stable and the backup retention window has passed, dropping legacy identity tables can be planned as a separate, explicitly destructive change.

## Interview Summary

“I used the strangler pattern. First I introduced a stable API Gateway, then extracted Identity as the first independently deployed business capability. Identity owns credentials and its database, while the content service trusts signed claims rather than querying another service's tables. The frontend contract did not change, data ownership became explicit, and the cutover retained the legacy tables for rollback. I also updated Compose, CI, health checks, tests, and the production runbook so this is an operable extraction rather than only a code split.”
