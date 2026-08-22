# Identity Service

The Identity Service is the first business capability extracted from the original backend. It independently owns:

- user registration and profile management
- password hashing and credential verification
- roles and administrator authorization
- JWT issuance with immutable `userId` and `roles` claims
- its own Flyway-managed `blog_identity` database

The public URLs remain unchanged because the API Gateway routes `/api/v1/auth/**` and `/api/users/**` to this service.

Run tests independently:

```bash
mvn -f identity-service/pom.xml test
```

For a manual non-Docker run, start MySQL on port `3307` and then run:

```bash
mvn -f identity-service/pom.xml spring-boot:run
```

The service listens on port `9092` by default. It is private when run through Docker Compose.
