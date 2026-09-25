# Azure Pipelines

The root `azure-pipelines.yml` is a CI pipeline for Azure DevOps Services. It:

1. Selects Java 17 on a Microsoft-hosted Ubuntu agent.
2. Caches dependencies and runs the repository's Maven wrapper with `clean verify`.
3. Publishes JUnit results even when tests fail.
4. Verifies and publishes JaCoCo results to the run's **Code Coverage** tab, and
   publishes the complete HTML report as the `jacoco-html` pipeline artifact.
5. Lints, renders and packages the Helm chart.
6. Builds the Docker image to validate the Dockerfile. The image stays on the agent
   and is not pushed or retained; the Dockerfile runs its own tests as well.
7. Publishes the executable JAR, packaged chart, rendered local manifests and API
   documentation as the `movies-build` pipeline artifact, only after success.

## Connect the pipeline

In Azure DevOps, choose **Pipelines → New pipeline**, select the repository, then
choose **Existing Azure Pipelines YAML file** and `/azure-pipelines.yml`. Save and
run it. The organization needs permission/capacity to run Microsoft-hosted agents.
No deployment credentials or application secrets are required for this CI job.

Pushes to all branches trigger builds. For Azure Repos pull-request validation,
add this pipeline as a **Build validation** policy on each protected target branch.
The YAML `pr` trigger applies to supported external repository providers such as
GitHub; Azure Repos uses branch policies instead.

## Scope and next deployment setup

This file does not push an image, deploy to Kubernetes, or enforce an 80% coverage
threshold. JaCoCo coverage is reported but is not currently used as a build gate.
Completing the assessment's delivery requirements still needs a coverage gate, a
registry service connection, a deployment target/service connection, environment
configuration, and a successful public deployment through the pipeline.
Keep those credentials in Azure DevOps service connections or secret variables.
Never place them directly in this YAML.

The rendered chart uses the disposable local database profile. It is a validation
artifact, not a production deployment configuration.

Task behaviour follows Microsoft's [Java tool installer](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/java-tool-installer-v0?view=azure-pipelines),
[Docker task](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/reference/docker-v2?view=azure-pipelines),
and [pipeline trigger guidance](https://learn.microsoft.com/en-us/azure/devops/pipelines/troubleshooting/troubleshoot-triggers?view=azure-devops).
