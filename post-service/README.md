# Post Service

The Post Service is the second business capability extracted from the original backend. It independently owns:

- post creation, update, deletion, search, and pagination
- scalar author ownership derived from Identity Service JWT claims
- category snapshots captured through the Category API
- post-image upload, validation, storage, and download
- a private post-reference API used by the remaining Comment module
- its own Flyway-managed `blog_posts` database

The public URLs remain unchanged because the API Gateway routes the existing post paths to this service. Comment paths such as `/api/posts/{postId}/comments` remain on the backend until Phase 5.

Run tests independently:

```bash
mvn -f post-service/pom.xml test
```

For a manual non-Docker run, start MySQL on port `3308`, the remaining backend on port `9091`, and then run:

```bash
mvn -f post-service/pom.xml spring-boot:run
```

The service listens on port `9093` by default and is private when run through Docker Compose.
