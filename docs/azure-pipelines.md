# Azure Pipelines

The root `azure-pipelines.yml` is a CI pipeline for Azure DevOps Services. It:

1. Selects Java 17 on a Microsoft-hosted Ubuntu agent.
2. Caches dependencies and runs the repository's Maven wrapper with `clean verify`.
3. Publishes JUnit results even when tests fail.
4. Verifies and publishes JaCoCo results to the run's **Code Coverage** tab, and
   publishes the complete HTML report as the `jacoco-html` pipeline artifact.
5. Lints, renders and packages the Helm chart.
6. Builds the Docker image to validate the Dockerfile. The Dockerfile runs its own
   tests as well.
7. Publishes the executable JAR, packaged chart, rendered local manifests and API
   documentation as the `movies-build` pipeline artifact, only after success.
8. On successful non-PR builds of `main`, runs a dedicated `BuildAndPushImage`
   stage that uses ACR Tasks to build and push the immutable
   `movies-api:$(Build.BuildId)` image.
9. Runs `DeployContainerApp` only after that push succeeds, deploying the exact
   immutable ACR image without rebuilding it.

## Connect the pipeline

In Azure DevOps, choose **Pipelines → New pipeline**, select the repository, then
choose **Existing Azure Pipelines YAML file** and `/azure-pipelines.yml`. Save and
run it. The organization needs permission/capacity to run Microsoft-hosted agents.
The build job needs no deployment credentials. The deployment job requires the
service connection and secret variables described below.

Pushes to all branches trigger builds. For Azure Repos pull-request validation,
add this pipeline as a **Build validation** policy on each protected target branch.
The YAML `pr` trigger applies to supported external repository providers such as
GitHub; Azure Repos uses branch policies instead.

## Container Apps deployment setup

Create or select these resources before the first deployment:

1. An Azure Resource Manager service connection named `sms-movies-azure` with
   permission to the target resource group.
2. A resource group named `sms-movies-rg`.
3. An Azure Container Registry. Change `acrName` in the pipeline because registry
   names are globally unique.
4. A Consumption-plan Container Apps environment named `sms-movies-env` in
   `eastus`. The deployment task creates or updates `sms-movies-api` with external
   ingress on port 8080, then limits it to 0.5 vCPU, 1 GiB of memory, zero minimum
   replicas and one maximum replica.
5. An Azure DevOps environment named `movies-production`. Add approvals there if
   production releases should require review.

Add these variables through **Pipelines → Library**, the pipeline variables UI, or
another secret store:

| Variable | Secret | Purpose |
| --- | --- | --- |
| `DATABASE_URL` | Yes | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Yes | Runtime database user |
| `DATABASE_PASSWORD` | Yes | Runtime database password |
| `APP_CORS_ALLOWED_ORIGINS` | No | Comma-separated frontend origins |

Do not place their values in YAML. Mark database values secret in Azure DevOps.
The deployment is intentionally restricted to successful `main` builds and does
not run for pull-request validation. Container Apps Consumption HTTP ingress uses
zero as its default minimum replica count, allowing the application to scale to
zero when idle.

JaCoCo enforces the repository's 80% line-coverage gate during `mvn verify` before
the deployment job can run.

The rendered chart uses the disposable local database profile. It is a validation
artifact, not a production deployment configuration.

Task behaviour follows Microsoft's [Java tool installer](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/java-tool-installer-v0?view=azure-pipelines),
[Docker task](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/docker-v2?view=azure-pipelines),
and [pipeline trigger guidance](https://learn.microsoft.com/en-us/azure/devops/pipelines/troubleshooting/troubleshoot-triggers?view=azure-devops).
