# Post Service

The Post Service is the second business capability extracted from the original backend. It independently owns:

- post creation, update, deletion, search, and pagination
- scalar author ownership derived from Identity Service JWT claims
- category snapshots captured through the Category API
- post-image upload, validation, provider-independent storage, and download
- a private post-reference API used by the remaining Comment module
- its own Flyway-managed `blog_posts` database

The public URLs remain unchanged because the API Gateway routes the existing post paths to this service. Comment paths such as `/api/posts/{postId}/comments` remain on the backend until Phase 5.

## Image Storage

The service uses the `ImageStorageService` boundary so HTTP and database code do not depend on a storage vendor:

- `local` is the default provider and stores images under `POST_IMAGE_PATH`
- `s3` is selected by adding the `aws` Spring profile and requires `POST_IMAGE_S3_BUCKET`
- both providers preserve the existing upload and download API, so the React frontend does not change
- JPEG, PNG, and WebP uploads are checked by declared content type, extension, size, and file signature
- S3 objects are private, use SSE-S3 encryption, and are accessed by the Post Service rather than public bucket URLs

The complete configuration, rollout, IAM, migration, and rollback design is documented in [AWS S3 image storage](../docs/aws-s3-image-storage.md).

Run tests independently:

```bash
mvn -f post-service/pom.xml test
```

For a manual non-Docker run, start MySQL on port `3308`, the remaining backend on port `9091`, and then run:

```bash
mvn -f post-service/pom.xml spring-boot:run
```

The service listens on port `9093` by default and is private when run through Docker Compose.
