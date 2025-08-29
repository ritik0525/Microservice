```markdown
# Using Docker Secrets and Kubernetes Secrets for the video microservices

This document shows how to convert environment variable secrets into Docker Secrets (Compose/Swarm) or Kubernetes Secrets.

Overview
- Docker secrets: secrets are mounted in containers at /run/secrets/<secret-name>. Use an entrypoint script to read the files and export environment variables for your application.
- Kubernetes secrets: create a Secret and either:
  - map secret keys into environment variables (envFrom: secretRef), or
  - mount as files into a volume and let the app read from the file path.

Recommendations
- Keep secrets out of repository. Use CI/CD secret stores, Docker secrets, or Kubernetes Secrets.
- For local dev use small secret files under ./secrets (gitignored) and docker-compose will mount them when supported, or use the stack deploy + docker secret create commands for Swarm.
- In production prefer your cloud provider's secret manager and inject secrets via orchestrator integration.

Sections
1) Docker Compose / Docker secrets (example)
2) Kubernetes Secrets (example)
3) Commands and examples
```