# Movies API

Spring Boot 4 / Java 17 movie-tracking backend with separate controllers for
accounts, movies, people, genres, collections, personal ratings, sharing, discovery,
audit history and analytics. Uses Spring Data JDBC, database-backed sessions and
Flyway migrations. [Frontend API contract](docs/frontend-api.md) documents all
routes, payloads, permissions, errors and the browser login flow.

## Run locally

With a JDK 17 or newer installed:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile initializes an in-memory H2 database and session tables, and
disables Docker Compose. Data disappears when the app stops. Swagger UI is at
`http://localhost:8080/swagger-ui/index.html`; OpenAPI is at `/v3/api-docs`.

Accounts are persisted in the database. Register and log in through the API; there
is no generated Spring password or default account. Before any write, get a CSRF
token from `/api/csrf` and retain the session cookie. Login rotates both the session
ID and CSRF token; fetch a fresh token afterwards. See the API guide for details.

For catalogue administration, optionally set `APP_ADMIN_EMAIL` and
`APP_ADMIN_PASSWORD` before startup. A new ADMIN account is created once, with a
12–128 character password. An existing non-admin account is never silently
promoted. Existing admin passwords are not reset. Supply bootstrap credentials
through environment/Secrets, then remove them once a persistent account is created.
With the disposable local database, bootstrap runs again after every app restart.

## Configuration

| Variable | Purpose |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` for the disposable development database |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Datasource configuration |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Optional initial administrator |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated exact frontend origins; default same-origin |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | Set true for HTTPS deployments |
| `SERVER_SERVLET_SESSION_COOKIE_SAME_SITE` | Default lax; assess explicitly for cross-site deployments |

Passwords and provider keys must not be committed. Public registration always
creates ordinary users. Catalogue mutations require ADMIN; collection and rating
ownership is checked against the current session. Share links grant read-only
access to their holders and can be expired or revoked.

## Docker

```sh
docker build -t movies-api:0.0.1-SNAPSHOT .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local movies-api:0.0.1-SNAPSHOT
```

The multi-stage build runs tests and packages the app, then uses a non-root Java 17
runtime container. It selects no profile by default.

## Kubernetes

The [Helm chart](helm/movies/README.md) includes a Deployment, Service, optional
ingress, probes and Secret references. Its default is one pod with the local
in-memory database. Make the image available to the cluster before installation.

## Azure Pipelines

[`azure-pipelines.yml`](azure-pipelines.yml) builds and tests the API with Java 17,
validates the Docker image and Helm chart, and publishes build artifacts. See the
[pipeline setup guide](docs/azure-pipelines.md) to connect it to Azure DevOps.
Registry publishing, deployment, and coverage enforcement are not configured yet.

## Database changes

Flyway migrations live in `src/main/resources/db/migration`. V1 creates the original
movie table; V2 adds accounts, normalized movie relationships, collections, ratings,
shares, audit, analytics and discovery request metadata. V2 backfills director and
genre relationships from existing movies. New database instances migrate on startup.
The local development profile does not need a manual schema script.

For an existing persistent database created before Flyway, back it up and verify
that its schema matches V1 before deliberately baselining at version 1. Automatic
baselining is disabled. No persistent database was modified by this implementation.

Only H2 is bundled and tested. Other databases require their JDBC/Flyway support,
dialect verification and session-schema provisioning. For production, use a
separate migration account and deny UPDATE/DELETE on audit records to the runtime
account; local H2 runs with broad development permissions. API audit access is
read-only. Apply forward corrective migrations rather than editing applied files.
Rollback requires a compatible application image or a tested database backup
restore; destructive down-migrations are not generated automatically.

## Tests

```sh
./mvnw test
```

Tests cover migrations, persisted CRUD, filters, session/CSRF rotation, logout,
account validation, ownership, rating integrity, membership/event idempotency,
sharing expiry/revocation, audit writes, analytics validation and local ranking.
The test profile uses H2; production-database and cluster tests remain separate.

## Discovery and integration limits

AI endpoints use a local TF-IDF content-based baseline with personalization and an
explicit fallback. They run without external credentials; they are not an LLM and
do not provide full semantic understanding. Corpus size is capped at 1000 movies.
Third-party ratings can be imported by an admin and displayed with their source
and scale, but no external movie-data provider is configured. The detailed API
guide describes these limitations and response contracts.

This API implementation does not complete every assessment deliverable. Frontend,
provider integration, coverage gate/CI deployment, product KPI analysis, consent
preference storage, retention jobs, distributed rate limiting, password recovery,
email verification and production operational controls still need separate work.
