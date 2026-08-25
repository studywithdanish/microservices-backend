# Content Service

The Content Service is the final business service extracted from the original blogging backend. It owns categories and comments on port `9094` and persists them in the independent `blog_content` database.

## API ownership

- `GET /api/categories` and `GET /api/categories/{categoryId}` are public.
- Category create, update, and delete operations require `ROLE_ADMIN`.
- `GET /api/posts/{postId}/comments` is public.
- Comment creation requires a valid Identity Service JWT.
- Comment deletion is restricted to the comment author or an administrator.
- The legacy `/api/comments/post/{postId}/comments` route remains compatible.

Comments store scalar `postId` and `authorId` values. The service validates a post through the Post Service private endpoint using `X-Internal-Service-Token`; it never reads Post or Identity tables.

## Run outside Docker

Start MySQL on port `3309`, Post Service on `9093`, and then run:

```bash
mvn spring-boot:run
```

Useful URLs:

```text
http://localhost:9094/actuator/health
http://localhost:9094/swagger-ui/index.html
http://localhost:9094/v3/api-docs
```

## Configuration

- `CONTENT_SERVER_PORT`
- `CONTENT_DB_URL`
- `CONTENT_DB_USERNAME`
- `CONTENT_DB_PASSWORD`
- `POST_SERVICE_BASE_URL`
- `CONTENT_HTTP_CONNECT_TIMEOUT`
- `CONTENT_HTTP_READ_TIMEOUT`
- `JWT_SECRET`
- `INTERNAL_SERVICE_TOKEN`
- `CORS_ALLOWED_ORIGINS`

`JWT_SECRET` and `INTERNAL_SERVICE_TOKEN` must be different strong secrets.
The HTTP timeout values default to `2s` for connection establishment and `5s` for the response, keeping dependency failures inside the gateway's response-timeout budget.

## Tests

```bash
mvn test
```
