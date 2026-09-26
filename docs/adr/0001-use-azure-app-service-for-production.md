# ADR 0001: Use Azure App Service for production hosting

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision owners:** Movies API maintainers

## Context

The Movies API is packaged as a single stateless Spring Boot container that listens
on port 8080. Persistent application data and HTTP sessions are stored in
PostgreSQL. The service has no requirement for Kubernetes-specific scheduling,
service discovery, sidecars, operators, or multiple independently deployable
components.

The repository contains a Helm chart, but operating a production Kubernetes
environment would also require a cluster, ingress controller, certificate and DNS
integration, secret management, monitoring, upgrades, capacity management, and a
shared production database. The chart's default local profile uses an in-memory H2
database and is intentionally restricted to one replica; it is not a production
configuration.

The existing Azure DevOps pipeline already builds an immutable container image,
pushes it to Azure Container Registry (ACR), and can deploy that image to an Azure
Linux Web App for Containers.

## Decision

Deploy the production Movies API to **Azure App Service using a Linux container**
instead of operating it on Kubernetes.

The production deployment will:

- build and test the application in Azure DevOps;
- publish an immutable `movies-api:<build-id>` image to ACR;
- use the App Service managed identity to pull the image from ACR;
- run the container with the `production` Spring profile on port 8080;
- supply database credentials and other runtime configuration through App Service
  application settings backed by the deployment environment; and
- connect to an externally managed PostgreSQL database over TLS.

The Helm chart remains available for local experimentation, validation, and a
possible future migration. It is not the production deployment target.

## Decision drivers

- The system is currently one deployable API rather than a set of independently
  scaled services.
- App Service provides the required container hosting, HTTPS endpoint, identity,
  restart, deployment, and scaling capabilities with less platform machinery.
- The existing delivery pipeline and ACR integration already support App Service.
- A managed application platform reduces the operational work required from this
  project's maintainers.
- Retaining standard OCI images avoids coupling the application binary to App
  Service and preserves a migration path.

## Alternatives considered

### Kubernetes with the repository Helm chart

Not selected for production at this stage. Kubernetes offers greater scheduling,
networking, and workload-control flexibility, but the application does not
currently need those capabilities. The additional cluster and add-on lifecycle
would increase operational responsibility without a corresponding product benefit.

### Azure Container Apps

Not selected because the existing App Service deployment path meets the current
requirements. Container Apps may be reconsidered if event-driven scaling,
revision-based traffic splitting, or a larger set of containerized services becomes
important.

### Virtual machine deployment

Not selected because it would require the team to manage the host operating system,
container runtime, patching, availability, and deployment integration directly.

## Consequences

### Positive

- The production topology and deployment process remain small and understandable.
- Azure manages the web-hosting infrastructure and integrates with ACR managed
  identity.
- Application deployment stays image-based and reproducible.
- The service can use App Service deployment slots and scale-out features later if
  required.

### Negative and constraints

- The deployment depends on App Service platform behavior and Azure-specific
  configuration.
- Kubernetes features and the Helm chart are not exercised by the production path.
- Scaling to multiple application instances requires a shared PostgreSQL database
  and correctly initialized Spring Session tables.
- The current API rate limiter is in-process, so limits are per instance. A shared
  limiter or trusted gateway must be introduced before relying on consistent limits
  across scaled-out instances.
- Health checks currently verify only that the application accepts connections;
  database-aware health reporting should be added before using automated health
  routing or aggressive scale-out.

## Reconsider this decision when

Re-evaluate the hosting platform if the system grows into multiple independently
deployed services, needs Kubernetes-specific controllers or sidecars, requires
portable multi-cloud orchestration, or App Service can no longer meet its scaling,
networking, availability, or deployment requirements.

## Implementation references

- [`azure-pipelines.yml`](../../azure-pipelines.yml)
- [`docs/azure-pipelines.md`](../azure-pipelines.md)
- [`Dockerfile`](../../Dockerfile)
- [`helm/movies/README.md`](../../helm/movies/README.md)
