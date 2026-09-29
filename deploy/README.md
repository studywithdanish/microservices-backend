# Production Deployment Runbook

This runbook deploys the API Gateway, Identity Service, Post Service, Content Service, Notification Service, Kafka, frontend, four MySQL databases, and Caddy reverse proxy on one AWS Lightsail or EC2 Ubuntu server.

The first deployment uses one public origin:

```text
https://your-domain.com
```

Caddy manages HTTPS certificates automatically. API traffic passes through the gateway before reaching private services:

```text
/                    -> React frontend
/api/v1/auth/**       -> API Gateway -> Identity Service -> Identity MySQL
/api/users/**         -> API Gateway -> Identity Service -> Identity MySQL
/api/posts            -> API Gateway -> Post Service -> Post MySQL
/api/post/**           -> API Gateway -> Post Service -> Post MySQL/images
/api/posts/*/comments  -> API Gateway -> Content Service -> Content MySQL
/api/comments/**       -> API Gateway -> Content Service -> Content MySQL
/api/categories/**     -> API Gateway -> Content Service -> Content MySQL
/api/notifications/**  -> API Gateway -> Notification Service -> Notification MySQL
PostPublished          -> Post Service -> Kafka -> Notification Service
/actuator/**         -> API Gateway health endpoints
```

This keeps the first deployment cost-effective and avoids managing a separate API subdomain before it is needed.

This runbook intentionally keeps post images on the existing Docker volume. The application also contains an optional, inactive S3 storage adapter for a later managed-storage rollout; see [AWS S3 image storage](../docs/aws-s3-image-storage.md) for its configuration, IAM, migration, and rollback design.

## 1. AWS Cost Controls

Before creating the server:

- Create an AWS budget alert around your monthly target.
- Use one small Lightsail/EC2 Ubuntu server.
- Do not create RDS, EKS, NAT Gateway, or Load Balancer for the first portfolio deployment.
- Open only required firewall ports: `22`, `80`, and `443`.

Updating the application later on the same server does not add fixed monthly cost. Cost changes only if you add larger infrastructure, storage, snapshots, high traffic, or managed services.

## 2. Server Setup

Install Docker and Docker Compose on Ubuntu:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo ${UBUNTU_CODENAME:-$VERSION_CODENAME}) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
```

Log out and log in again after adding your user to the Docker group.

## 3. Clone Both Repositories

Clone both repositories as sibling folders:

```bash
sudo mkdir -p /opt/blog-platform
sudo chown -R $USER:$USER /opt/blog-platform
cd /opt/blog-platform
git clone https://github.com/studywithdanish/microservices-backend.git
git clone https://github.com/studywithdanish/microservices-frontend.git
```

Expected layout:

```text
/opt/blog-platform/microservices-backend
/opt/blog-platform/microservices-frontend
```

## 4. Configure Environment

Create the deployment environment file:

```bash
cd /opt/blog-platform/microservices-backend/deploy
cp .env.production.example .env
nano .env
```

For first smoke test with a server IP before HTTPS is enabled:

```text
PUBLIC_FRONTEND_ORIGIN=http://SERVER_PUBLIC_IP
PUBLIC_API_BASE_URL=http://SERVER_PUBLIC_IP
```

After DNS is pointed to the server:

```text
PUBLIC_DOMAIN=your-domain.com
PUBLIC_FRONTEND_ORIGIN=https://your-domain.com
PUBLIC_API_BASE_URL=https://your-domain.com
```

Use strong values for:

```text
IDENTITY_MYSQL_ROOT_PASSWORD
IDENTITY_DB_PASSWORD
POST_MYSQL_ROOT_PASSWORD
POST_DB_PASSWORD
CONTENT_MYSQL_ROOT_PASSWORD
CONTENT_DB_PASSWORD
NOTIFICATION_MYSQL_ROOT_PASSWORD
NOTIFICATION_DB_PASSWORD
JWT_SECRET
INTERNAL_SERVICE_TOKEN
```

Generate a strong JWT secret:

```bash
openssl rand -base64 64
```

## 5. Deploy

Build and start the stack:

```bash
cd /opt/blog-platform/microservices-backend/deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

Check containers:

```bash
docker compose -f docker-compose.prod.yml --env-file .env ps
```

Check logs:

```bash
docker compose -f docker-compose.prod.yml --env-file .env logs -f identity-service
docker compose -f docker-compose.prod.yml --env-file .env logs -f post-service
docker compose -f docker-compose.prod.yml --env-file .env logs -f content-service
docker compose -f docker-compose.prod.yml --env-file .env logs -f notification-service
docker compose -f docker-compose.prod.yml --env-file .env logs -f kafka
docker compose -f docker-compose.prod.yml --env-file .env logs -f gateway
docker compose -f docker-compose.prod.yml --env-file .env logs -f reverse-proxy
```

## 6. Verify

Use these URLs:

```text
https://your-domain.com/
https://your-domain.com/actuator/health
```

Then test from the React frontend:

```text
Create account
Login
```

## 7. Update Existing Deployment

Updating later uses the same server and does not add fixed monthly cost.

```bash
cd /opt/blog-platform/microservices-backend
git pull
cd /opt/blog-platform/microservices-frontend
git pull
cd /opt/blog-platform/microservices-backend/deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
docker image prune -f
```

## 8. Rollback

If a new deployment has an issue, check the latest known good commit:

```bash
cd /opt/blog-platform/microservices-backend
git log --oneline
git checkout <previous-backend-commit>
cd /opt/blog-platform/microservices-frontend
git log --oneline
git checkout <previous-frontend-commit>
cd /opt/blog-platform/microservices-backend/deploy
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

After recovery, return to `main` when ready:

```bash
git checkout main
```

## 9. DNS And HTTPS

Point these DNS records to the server static IP:

```text
A  @    SERVER_STATIC_IP
A  www  SERVER_STATIC_IP
```

Keep the DNS records in DNS-only mode while Caddy issues certificates directly from Let's Encrypt. Open `80` and `443` in the Lightsail firewall.

## 10. Interview Explanation

I deployed the project as a Docker Compose stack on one AWS server. Caddy terminates HTTPS and the API Gateway provides a stable routing boundary. Identity, Posts, Content, and Notifications are independently owned across four services and databases. Post Service uses a transactional outbox to publish versioned events to Kafka; Notification Service consumes them idempotently with retry and dead-letter handling. Services validate JWT claims locally, use Flyway, and receive secrets through environment variables. The gateway has explicit route ownership and no catch-all legacy backend. Database, Kafka, uploaded-image, and TLS data use persistent Docker volumes. I avoided EKS, RDS, NAT Gateways, and load balancers for the first portfolio deployment to control cost while keeping the architecture ready for later automation.

## 11. Switching From IP Smoke Test To Domain

```text
PUBLIC_DOMAIN=your-domain.com
PUBLIC_FRONTEND_ORIGIN=https://your-domain.com
PUBLIC_API_BASE_URL=https://your-domain.com
```

Then rebuild the frontend and reverse proxy:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build frontend reverse-proxy
```
