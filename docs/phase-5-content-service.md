# Phase 5: Content Service Extraction

## Goal

Extract Categories and Comments into an independently deployed Content Service and database. After this cutover, the original backend is no longer part of the active runtime; its source and database remain temporarily as rollback assets.

## Final Runtime Boundary

```text
Browser
  |
  v
API Gateway :9090
  |-- identity paths         -> Identity Service :9092 -> Identity MySQL
  |-- post and image paths   -> Post Service :9093     -> Post MySQL + image volume
  `-- category/comment paths -> Content Service :9094  -> Content MySQL
                                      |
                                      `-- private post-reference call -> Post Service
```

There are no shared business tables. Services communicate only through explicit HTTP contracts and verified identity claims.

## Gateway Route Ownership

Identity Service owns:

- `/api/v1/auth/**`
- `/api/users/**`

Post Service owns:

- `/api/posts`
- `/api/posts/search/**`
- `/api/post/**`
- `/api/user/{userId}/posts`
- `/api/user/{userId}/category/{categoryId}/posts`
- `/api/category/{categoryId}/posts`

Content Service owns:

- `/api/categories/**`
- `/api/posts/{postId}/comments`
- `/api/comments/**`

The gateway no longer forwards `/api/**` to a catch-all backend. Unknown and `/internal/**` paths return `404` at the public boundary.

## Data Ownership

Content Service owns:

- `service_categories`: category ID, title, and description
- `service_comments`: comment ID, content, scalar post ID, and scalar author ID

There are no foreign keys to Post or Identity databases. The Post Service continues to store category title/description snapshots in posts, avoiding cross-service joins and N+1 category calls. Existing post snapshots are eventually consistent after a category rename; an asynchronous category-updated event is a deliberate future enhancement, not a shared-database shortcut.

## Security and Service Communication

- Category reads and comment reads are public.
- Category mutations require a JWT with `ROLE_ADMIN`.
- Comment creation requires a valid Identity JWT.
- Comment deletion requires the comment owner or an administrator.
- Content Service verifies post IDs with `GET /internal/posts/{postId}/reference`.
- The private request carries `X-Internal-Service-Token` and never passes through the gateway.
- Services verify signed JWT identity/role claims locally and do not trust client-supplied owner IDs.

## Existing Data Migration

For a new environment, Flyway creates the Content schema and categories can be added normally.

For an existing environment:

1. Temporarily pause category and comment writes.
2. Back up the legacy content database and the new Content database.
3. Copy `catgories` rows into `service_categories`, preserving `category_id`, `title`, and `description`.
4. Copy `comments` rows into `service_comments`, preserving `id`, `content`, `post_post_id` as `post_id`, and `author_id`.
5. Set both new auto-increment values above the highest imported IDs.
6. Compare source and destination row counts and sample records.
7. Verify every migrated comment's `post_id` exists through the Post Service private reference API.
8. Deploy Content Service and confirm `/actuator/health` is `UP`.
9. Point Post Service `CATEGORY_SERVICE_BASE_URL` to Content Service.
10. switch the gateway's category/comment routes and run the smoke tests.
11. Resume writes.

Preserving category IDs is essential because Post Service stores them in category snapshots. Preserving comment and post IDs keeps existing URLs and relationships stable.

## Verification

```bash
mvn clean test
mvn -f identity-service/pom.xml clean test
mvn -f post-service/pom.xml clean test
mvn -f content-service/pom.xml clean test
mvn -f gateway-service/pom.xml clean test
docker compose config
docker compose up --build
```

The completed implementation currently passes 88 automated tests across the retained rollback backend, Identity Service, Post Service, Content Service, and API Gateway.

Through `http://localhost:9090`, verify:

1. registration and login still route to Identity Service;
2. public category reads route to Content Service;
3. category writes reject normal users and accept administrators;
4. authenticated post creation resolves its category through Content Service;
5. authenticated comment creation validates its post through the private service credential;
6. comment reads are public and owner/admin deletion is enforced;
7. `/internal/**` and unknown APIs return `404` at the gateway;
8. stopping Post Service causes comment validation to return `503` instead of corrupting data.

## Rollback

If Phase 5 fails:

1. Stop new category/comment writes.
2. Route category/comment paths back to the retained legacy backend image.
3. Point Post Service category lookup back to the legacy backend.
4. Reconcile categories and comments created during the Phase 5 window.
5. Restore the legacy database backup only if reconciliation is not sufficient.
6. Diagnose and retry the cutover.

Do not drop the legacy category/comment tables or delete the rollback image in the extraction release. Their removal is a separate destructive change after the stability and backup-retention windows close.

## Interview Summary

“In Phase 5 I completed the strangler migration by extracting Categories and Comments into a Content Service with its own database. The gateway now uses explicit routes only, the old backend is absent from the active runtime, and no service shares business tables. Comments validate Post IDs through a private token-protected endpoint, while authorization comes from verified Identity JWT claims. I preserved IDs during migration and retained the previous backend only as a temporary rollback asset.”
