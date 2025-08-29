# PowerShell helper: create Docker secrets for Swarm from local files (Windows PowerShell)
# Usage:
#   .\create-secrets.ps1 -SecretName jwt_access_secret -FilePath .\secrets\jwt_access_secret.txt
param(
  [Parameter(Mandatory=$true)][string]$SecretName,
  [Parameter(Mandatory=$true)][string]$FilePath
)

if (-not (Test-Path $FilePath)) {
  Write-Error "File not found: $FilePath"
  exit 1
}

# docker secret create accepts a file path directly on most platforms. On Windows it's often simplest:
#   docker secret create <name> <file>
# If you need to pipe bytes, you can use Get-Content -Raw | docker secret create <name> -
Write-Host "Creating Docker secret '$SecretName' from file '$FilePath' ..."
docker secret create $SecretName $FilePath
Write-Host "Done."