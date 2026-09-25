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
   stage that builds on the Microsoft-hosted agent and pushes the immutable
   `movies-api:$(Build.BuildId)` image to ACR. It does not use server-side ACR
   Tasks, which can be unavailable on free subscriptions.
9. Runs `DeployContainerApp` after the image push, deploying that exact immutable
   tag with external ingress on port 8080. It limits the app to 0.5 vCPU, 1 GiB of
   memory, zero minimum replicas and one maximum replica.

## Connect the pipeline

In Azure DevOps, choose **Pipelines → New pipeline**, select the repository, then
choose **Existing Azure Pipelines YAML file** and `/azure-pipelines.yml`. Save and
run it. The organization needs permission/capacity to run Microsoft-hosted agents.
The build-and-push stage requires the service connection described below.

Pushes to all branches trigger builds. For Azure Repos pull-request validation,
add this pipeline as a **Build validation** policy on each protected target branch.
The YAML `pr` trigger applies to supported external repository providers such as
GitHub; Azure Repos uses branch policies instead.

## Azure deployment setup

Create or select these resources before the first image push:

1. An Azure Resource Manager service connection named `smsmovies2` with
   the `AcrPush` role on the registry.
2. An Azure Container Registry matching `acrName` in the pipeline. Registry names
   are globally unique.
3. A resource group named `sms-movies` and a Consumption-plan Container Apps
   environment named `sms-movies-env` in `eastus`.
4. An Azure DevOps environment named `movies-production`. Configure approvals on
   this environment if releases should require review.

Add `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and
`APP_CORS_ALLOWED_ORIGINS` as Azure DevOps pipeline variables. Mark the three
database values secret. Do not place their values directly in YAML.

The image push and deployment are restricted to successful `main` builds and do
not run for pull-request validation.

JaCoCo enforces the repository's 80% line-coverage gate during `mvn verify` before
the image build-and-push and deployment stages can run.

The rendered chart uses the disposable local database profile. It is a validation
artifact, not a production deployment configuration.

Task behaviour follows Microsoft's [Java tool installer](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/java-tool-installer-v0?view=azure-pipelines),
[Docker task](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/docker-v2?view=azure-pipelines),
and [pipeline trigger guidance](https://learn.microsoft.com/en-us/azure/devops/pipelines/troubleshooting/troubleshoot-triggers?view=azure-devops).
