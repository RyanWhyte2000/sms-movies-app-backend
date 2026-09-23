# Movies Helm chart

Requires Helm 3+ and Kubernetes 1.23+. This chart deploys the existing Docker image;
it does not build images or provision a database. Commands below run from the
repository root. The default profile is `local`, with one pod and an in-memory H2
database. Data is lost on restart or upgrade. Local deployments use the Recreate
strategy and reject multiple replicas to avoid separate in-memory databases.

## Build and make the image available

```sh
docker build -t movies-api:0.0.1-SNAPSHOT .
```

For a local kind cluster, load the image into the cluster (supply `--name` if needed):

```sh
kind load docker-image movies-api:0.0.1-SNAPSHOT
```

For minikube, use `minikube image load movies-api:0.0.1-SNAPSHOT` instead.
For a remote cluster, tag and push to your registry and override `image.repository`
and `image.tag` during installation. Use `imagePullSecrets` for a private registry.
Build for your cluster's CPU architecture when it differs from your workstation.

## Install and access

```sh
helm lint ./helm/movies
helm template movies ./helm/movies
helm upgrade --install movies ./helm/movies \
  --namespace movies --create-namespace --wait --timeout 5m
kubectl -n movies port-forward service/movies-movies 8080:8080
```

Register and sign in through the API. There is no generated development password.
Follow the [frontend API guide](../../docs/frontend-api.md) for session cookies,
CSRF tokens, registration and login. No resources are installed until you run the
install command.

## Configure

Keep overrides in a separate values file and pass it using `-f my-values.yaml`.

| Value | Purpose |
| --- | --- |
| `image.repository`, `image.tag`, `image.pullPolicy` | Container image settings |
| `imagePullSecrets` | List of `{name: registry-secret}` entries |
| `replicaCount` | Pod count; local profile allows only one |
| `springProfilesActive` | Spring profiles, default `local` |
| `env` | Map of non-secret environment variables |
| `existingSecrets` | List of existing Secret names to import as environment variables |
| `service.type`, `service.port` | Service exposure, default ClusterIP on 8080 |
| `ingress` | Optional ingress hosts, controller class, annotations, and TLS |
| `resources` | CPU and memory requests and limits |
| `startupProbe`, `readinessProbe`, `livenessProbe` | Override probes or set null to disable |
| `nodeSelector`, `tolerations`, `affinity` | Pod scheduling |
| `podAnnotations` | Pod annotations, including manual restart markers |

The container port is fixed at 8080. Set the profile through `springProfilesActive`,
not through `env` or Secrets. Avoid chart-managed environment names in overrides.

For optional administrator bootstrap, create a Secret in the deployment namespace using your
normal secret management workflow. Its keys should be
`APP_ADMIN_EMAIL` and `APP_ADMIN_PASSWORD`, then reference it:

```yaml
existingSecrets:
  - movies-credentials
```

The chart does not store passwords in its values or create credential Secrets.
Restart the deployment after changing referenced Secrets:

```sh
kubectl -n movies rollout restart deployment/movies-movies
```

To enable ingress with an installed controller and an existing TLS Secret:

```yaml
ingress:
  enabled: true
  className: nginx
  hosts:
    - host: movies.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: movies-tls
      hosts:
        - movies.example.com
```

Configure DNS and provision `movies-tls` separately. The chart does not install an
ingress controller or issue certificates.

## Persistent deployments

Before changing the profile or scaling, add the required database driver to the
application, configure a shared datasource, and provide movie and Spring Session
schema migrations. The current application only includes H2 and H2-tested Flyway migrations.
Pass datasource credentials through an existing Secret. Simply setting
`springProfilesActive: production` does not provide database configuration.

Pods run as UID/GID 10001, with a read-only root filesystem, writable temporary
storage, dropped capabilities, and no mounted Kubernetes API token. Probes use TCP
because the current API requires authentication and has no Actuator health endpoint.
They check whether the listener accepts connections, not database health.

## Remove

```sh
helm uninstall movies --namespace movies
```

This removes chart-managed resources; externally managed Secrets remain.
