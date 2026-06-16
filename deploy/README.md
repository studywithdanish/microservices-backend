# Production Deployment Runbook

This runbook deploys the backend, frontend, MySQL, and Nginx reverse proxy on one low-cost AWS Lightsail or EC2 Ubuntu server.

The first deployment uses one public origin:

```text
http://your-domain.com
```

Nginx routes traffic internally:

```text
/                    -> React frontend
/api/**              -> Spring Boot backend
/actuator/**         -> Spring Boot backend
/swagger-ui/**       -> Spring Boot backend
/v3/api-docs         -> Spring Boot backend
```

This keeps the first deployment cost-effective and avoids managing a separate API subdomain before it is needed.

## 1. AWS Cost Controls

Before creating the server:

- Create an AWS budget alert around your monthly target.
- Use one small Lightsail/EC2 Ubuntu server.
- Do not create RDS, EKS, NAT Gateway, or Load Balancer for the first portfolio deployment.
- Open only required firewall ports: `22` and `80` initially, `443` after SSL.

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

For first smoke test with a server IP:

```text
PUBLIC_FRONTEND_ORIGIN=http://SERVER_PUBLIC_IP
PUBLIC_API_BASE_URL=http://SERVER_PUBLIC_IP
```

After DNS and SSL:

```text
PUBLIC_FRONTEND_ORIGIN=https://your-domain.com
PUBLIC_API_BASE_URL=https://your-domain.com
```

Use strong values for:

```text
MYSQL_ROOT_PASSWORD
DB_PASSWORD
JWT_SECRET
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
docker compose -f docker-compose.prod.yml --env-file .env logs -f backend
docker compose -f docker-compose.prod.yml --env-file .env logs -f reverse-proxy
```

## 6. Verify

Use these URLs:

```text
http://SERVER_PUBLIC_IP/
http://SERVER_PUBLIC_IP/actuator/health
http://SERVER_PUBLIC_IP/swagger-ui/index.html
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

## 9. Interview Explanation

I deployed the project as a cost-conscious Docker Compose stack on one AWS server. Nginx is the public entry point and routes frontend and backend traffic internally. The backend runs with the production profile, uses Flyway for database schema versioning, and stores secrets in environment variables. MySQL data and uploaded images use persistent Docker volumes. I avoided EKS, RDS, and load balancers for the first portfolio deployment to control cost, while keeping the architecture ready for Jenkins CD, Terraform, monitoring, and later Kubernetes migration.

## 10. HTTPS Follow-Up

First verify the application over HTTP using the server IP or domain. After DNS is stable, add HTTPS and update:

```text
PUBLIC_FRONTEND_ORIGIN=https://your-domain.com
PUBLIC_API_BASE_URL=https://your-domain.com
```

Then rebuild the frontend and reverse proxy:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d --build frontend reverse-proxy
```

HTTPS can be added with a certificate-managed reverse proxy step after the first HTTP deployment is healthy.
