```markdown
# Commands: Docker secrets / Swarm / Kubernetes

Docker Secrets (Swarm)
1) Create secrets from files (Swarm):
   docker secret create jwt_access_secret secrets/jwt_access_secret.txt
   docker secret create signedurl_secret secrets/signedurl_secret.txt

2) Deploy stack (uses secrets created above):
   docker stack deploy -c docker-compose.secrets.yml video-stack

Notes:
- docker-compose up (classic) does not persist secrets into the Swarm store; to use docker secret create + secrets that are managed by Docker you must use Swarm mode and docker stack deploy.
- For local dev you can keep secrets in ./secrets and Compose file references them via file: ... (Compose will mount them into containers under /run/secrets).

Docker Compose (non-swarm, local dev)
- Keep secrets files under ./secrets (gitignored).
- Start with:
  docker compose -f docker-compose.secrets.yml up --build

The Compose plugin will mount the files to /run/secrets/<name> for the container (supported by docker compose v2 plugin).

Kubernetes
1) Apply secret manifest:
   kubectl apply -f k8s/secret.yaml

2) Deploy services:
   kubectl apply -f k8s/deployment-upload.yaml
   kubectl apply -f k8s/deployment-streaming.yaml

3) Create from literal or file via CLI:
   kubectl create secret generic video-service-secrets \
     --from-literal=SECURITY_JWT_ACCESS_SECRET="$(cat secrets/jwt_access_secret.txt)" \
     --from-literal=SIGNEDURL_SECRET="$(cat secrets/signedurl_secret.txt)"

Read the k8s deployment manifests to decide whether to inject secrets as env vars (envFrom secretRef) or mount as files (use volume + volumeMount from secret).

Security tips
- Avoid committing secret files to git (add to .gitignore).
- Use a real secret manager (HashiCorp Vault, AWS Secrets Manager) in production and let your orchestrator inject secrets at runtime.
- Rotate secrets regularly. When rotating secrets used by multiple services ensure zero-downtime rollout (e.g., rolling update of deployments).
```