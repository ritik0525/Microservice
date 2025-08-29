```markdown
# Using Docker secrets with docker-compose V2 on Windows

This guide shows practical examples for using Docker secrets with the Docker Compose V2 plugin on Windows. It covers two common workflows:

1. Local Compose (Docker Desktop / docker compose) using secret files from your project (developer/dev machine).
2. Docker Swarm mode with `docker secret create` + `docker stack deploy` (production-like).

It also shows PowerShell / CMD commands and a small entrypoint pattern (used in the upload-service example) to expose secrets as environment variables for Spring Boot.

Important background notes
- Docker Desktop for Windows runs Linux containers inside a VM (WSL2 by default). Files in your Windows filesystem are accessible to containers but you must ensure file sharing/WSL integration is configured in Docker Desktop.
- For best developer experience and to avoid permission/line-ending issues, prefer working from WSL2 (Ubuntu distribution) and keeping your project files under the WSL filesystem (e.g. /home/you/project). If you prefer PowerShell/CMD, the examples below work — be mindful of CRLF line endings and file encoding (use UTF-8 without BOM).
- `docker compose` (Compose V2) supports mounting local secret files into containers when declared with `file:`. This does not require Swarm mode. `docker stack deploy` uses Docker secrets stored in the Swarm secret store (managed with `docker secret create`) — those are referenced as `external: true` in the compose.

What you get in this guide
- docker-compose example using local secret files (works with Docker Desktop + docker compose)
- a tiny entrypoint script (upload-service/entrypoint.sh) to load secrets (from /run/secrets/*) into env vars consumed by Spring Boot
- PowerShell and CMD scripts to create swarm secrets (if you want Swarm)
- instructions for using WSL2 and docker compose from WSL
- notes on security and best practices

Quick summary (commands)
- Local compose (file-based secrets):
  1. Create secret files in ./secrets/ (gitignored)
  2. docker compose -f docker-compose.windows.yml up --build -d

- Swarm mode (Docker secrets stored in Swarm):
  1. docker swarm init (on manager node)
  2. docker secret create jwt_access_secret ./secrets/jwt_access_secret.txt
  3. docker stack deploy -c docker-compose.swarm.yml video-stack

- Using PowerShell to create a secret from file:
  Get-Content -Raw .\secrets\jwt_access_secret.txt | docker secret create jwt_access_secret -

- Using WSL2 (recommended dev flow):
  1. Open WSL shell in project directory
  2. docker compose -f docker-compose.windows.yml up --build -d

Read the full examples and details below.
```