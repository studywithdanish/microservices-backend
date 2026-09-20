# Local Kubernetes Runbook

This directory deploys the complete platform to a dedicated local Minikube profile. It keeps the frontend and API Gateway public while the Identity, Post, Content, and MySQL services remain cluster-internal.

## Prerequisites

- Docker Desktop
- `kubectl`
- Minikube
- PowerShell 7+
- At least 6 GiB available to the Minikube profile

The default profile name is `blog-platform`. A dedicated profile avoids overwriting or resizing an existing Minikube environment.

## Create the cluster

```powershell
minikube start -p blog-platform --driver=docker --memory=6144 --cpus=4
```

Minikube makes `blog-platform` the current `kubectl` context. Confirm it before deployment:

```powershell
kubectl config current-context
```

## Build and deploy

From the backend repository root:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -Profile blog-platform
```

The script performs these steps:

1. Builds the five active Spring Boot service images and the React/Nginx image.
2. Loads all images into the selected Minikube profile.
3. Generates a Kubernetes Secret only when one does not already exist.
4. Deploys four MySQL StatefulSets and one Kafka StatefulSet with persistent volumes.
5. Waits for the databases and Kafka before deploying the services, gateway, and frontend.
6. Waits until every application Deployment is available.

To reuse images that are already loaded:

```powershell
.\deploy\k8s\scripts\deploy-local.ps1 -SkipImageBuild -Profile blog-platform
```

`secret.example.yaml` documents the required secret keys, but is deliberately excluded from Kustomize and must never contain real credentials.

## Access and verify

With the Docker driver on Windows, use a port-forward in one terminal:

```powershell
kubectl -n blog-platform port-forward service/frontend 18080:80
```

Then open `http://localhost:18080` or run the full cross-service smoke test from another terminal:

```powershell
.\deploy\k8s\scripts\smoke-test.ps1 -BaseUrl http://localhost:18080 -Profile blog-platform
```

The smoke test verifies gateway health, registration, login, the authenticated profile, categories, post creation, and comment creation.

Useful diagnostics:

```powershell
kubectl -n blog-platform get pods,services,persistentvolumeclaims
kubectl -n blog-platform logs deployment/api-gateway
kubectl -n blog-platform logs deployment/identity-service
kubectl -n blog-platform logs deployment/post-service
kubectl -n blog-platform logs deployment/content-service
kubectl -n blog-platform logs deployment/notification-service
kubectl -n blog-platform logs statefulset/kafka
```

## Validate manifests without deploying

```powershell
kubectl kustomize deploy/k8s
```

This renders and validates the Kustomize structure without requiring a live cluster or kubeconfig. The backend Jenkins pipeline runs this check along with `docker compose config --quiet`.

When a cluster is available, add a client-side apply check:

```powershell
kubectl apply --dry-run=client --validate=false -k deploy/k8s
```

## Stop or remove the environment

Stop the profile while preserving its volumes:

```powershell
minikube stop -p blog-platform
```

Deleting the profile permanently removes its local Kubernetes workloads and data:

```powershell
minikube delete -p blog-platform
```
