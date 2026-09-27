# Phase 4: Post Service Extraction

## Goal

Extract posts and post images into an independently deployed service and database while preserving all frontend paths. Categories and Comments remain in the backend until Phase 5.

## Runtime Boundary

```text
Browser
  |
  v
API Gateway :9090
  |-- identity paths -> Identity Service :9092 -> Identity MySQL
  |-- post paths     -> Post Service :9093     -> Post MySQL + image volume
  `-- comment/category paths -> Remaining Backend -> Content MySQL
                                  |             ^
                                  `-- private post-reference call
```

The Post Service calls the public Category API when creating a post. The remaining Comment module calls the private Post reference endpoint when it must validate a post ID. Neither service reads another service's tables.

## Gateway Route Ownership

The Post Service owns:

- `POST|GET /api/posts`
- `GET /api/posts/search/**`
- `GET|PUT|DELETE /api/post/{postId}`
- `POST /api/post/image/upload/{postId}`
- `GET /api/post/image/{imageName}`
- `GET /api/user/{userId}/posts`
- `POST /api/user/{userId}/category/{categoryId}/posts`
- `GET /api/category/{categoryId}/posts`

The remaining backend still owns:

- `POST|GET /api/posts/{postId}/comments`
- `/api/comments/**`
- `/api/categories/**`

The specific Post route is evaluated before the backend catch-all. The comment path does not match the exact `/api/posts` Post Service route.

## Data Ownership

`service_posts` stores:

- post content and image name
- scalar `author_id` from the verified Identity JWT
- scalar `category_id`
- category title and description snapshots

The category snapshot preserves the existing nested `category` response without a database join or an HTTP call for every post in a page. A category rename is eventually consistent for existing posts during this transitional phase. Phase 5 can publish a category-updated event to refresh snapshots.

The old `posts` table remains in the backend database only for rollback. It is no longer mapped by the running backend. Flyway migration `V3__decouple_comments_from_extracted_posts.sql` removes the comments-to-posts foreign key so new Post Service IDs can be referenced by Comments without sharing a database.

## Private Service Authentication

The Comment module requests:

```text
GET /internal/posts/{postId}/reference
X-Internal-Service-Token: <shared internal credential>
```

The endpoint is not routed by the public gateway. The Post Service compares the credential in constant time and requires `ROLE_INTERNAL`. Use a strong `INTERNAL_SERVICE_TOKEN` that is different from `JWT_SECRET`.

## Existing Data Migration

For a new local environment, create posts normally after creating at least one category.

For an existing environment:

1. Back up the content and Post databases and the image volume.
2. Export posts joined with their category title and description.
3. Import them into `service_posts`, preserving `post_id`, `user_id`, `category_id`, timestamps, and image names.
4. Set the `service_posts` auto-increment value above the highest imported post ID.
5. Move or attach the existing image volume to the Post Service.
6. Compare row counts and sample nested category responses.
7. Apply backend Flyway migration `V3` to remove the comment foreign key.
8. Deploy Post Service and confirm its health.
9. Verify the private reference endpoint from the backend network.
10. Switch the Gateway Post route and run the smoke tests.

Preserving post IDs is essential because existing comments store scalar post IDs.

## Verification

```bash
mvn test
mvn -f identity-service/pom.xml test
mvn -f post-service/pom.xml test
mvn -f gateway-service/pom.xml test
docker compose config
docker compose up --build
```

After registering and logging in through the gateway with `curl -c cookies.txt`, use the session cookie to create a post:

```bash
curl -i -X POST http://localhost:9090/api/posts \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"title":"Phase 4","content":"Posts are independently owned","categoryId":1}'
```

Then verify routing and integration:

```bash
curl -i http://localhost:9090/api/posts
curl -i http://localhost:9090/api/post/1
curl -i http://localhost:9090/api/posts/1/comments
curl -i -X POST http://localhost:9090/api/posts/1/comments \
  -b cookies.txt \
  -H "Content-Type: application/json" \
  -d '{"content":"The Comment module validated this through Post Service"}'
```

Expected behavior:

- post reads remain public
- post writes require a valid Identity token
- owner-or-admin authorization protects mutations and images
- comment creation succeeds for a Post Service ID
- missing posts return `404`
- unavailable Post Service returns `503` from comment operations
- `/internal/**` is inaccessible through the gateway

## Rollback

If the extraction fails:

1. Route Post paths back to the previous backend image.
2. Redeploy the previous backend image and schema behavior.
3. Restore the old image volume attachment.
4. Reconcile posts created during the Phase 4 window before retrying.
5. Keep the new database and legacy tables until the rollback window closes.

Do not drop legacy post tables in the extraction release. Their removal is a separate destructive migration after Phase 4 is stable.

## Interview Summary

“In Phase 4 I extracted Posts and images behind the existing gateway. I preserved the frontend contract, moved data ownership to a new database, used signed identity claims for author authorization, and replaced shared category and comment relationships with explicit APIs. Category data is stored as a snapshot to avoid N+1 calls, while Comments validate post IDs through a private service credential. I preserved IDs during migration and retained the old tables for rollback.”
