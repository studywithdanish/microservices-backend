# Local End-to-End Verification

This runbook verifies the complete application locally without AWS.

## Prerequisites

- Docker Desktop
- Git
- Node.js 20+ and npm 10+
- The backend and frontend repositories checked out beside each other

## 1. Start the backend

From `microservices-backend`:

```powershell
docker compose up --build -d
docker compose ps
```

Wait until the four databases, Kafka, four business services, and gateway report healthy or running. Verify the public entry point:

```powershell
Invoke-RestMethod http://localhost:9090/actuator/health
```

Expected result: `status` is `UP`.

Flyway adds a `General` category only when the Content database has no categories. This makes a fresh local environment usable while preserving existing category data.

## 2. Start the frontend

From `microservices-frontend`:

```powershell
Copy-Item .env.example .env
npm ci
npm start
```

Open `http://localhost:3000`. The default environment points the browser to the gateway at `http://localhost:9090`.

## 3. Exercise the user journey

1. Create an account with a unique email and an 8–72 character password.
2. Log in using that email and password.
3. Confirm the dashboard displays the Identity profile, Content category count, and recent Post count.
4. Create a post using the available category.
5. Load comments for the new post.
6. Add a comment and confirm it appears immediately.
7. Open the gateway health link and confirm `UP`.
8. Log out and confirm `/dashboard` redirects to `/login`.

This single journey crosses all active business services through the gateway.

## 4. Automated checks

Backend repository:

```powershell
mvn clean test
mvn -f gateway-service/pom.xml clean test
mvn -f identity-service/pom.xml clean test
mvn -f post-service/pom.xml clean test
mvn -f content-service/pom.xml clean test
mvn -f notification-service/pom.xml clean test
docker compose config
```

Frontend repository:

```powershell
npm run test:ci
npm run build
npm run security:audit
```

## 5. Stop the environment

```powershell
docker compose down
```

Use `docker compose down -v` only when you intentionally want to remove local databases, Kafka data, and uploaded images.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Frontend reports a gateway load error | `docker compose ps` and `http://localhost:9090/actuator/health` |
| Browser reports CORS failure | Frontend origin is included in `CORS_ALLOWED_ORIGINS` |
| Login returns 401 | Email/password are correct and the Identity Service is healthy |
| No category is available | Content migration `V2__seed_initial_general_category.sql` ran, or the existing database has a category |
| Post/comment returns 503 | Post and Content services can reach each other and share `INTERNAL_SERVICE_TOKEN` |
| A previous JWT stops working after configuration changes | Log out and log in again to obtain a newly signed token |
