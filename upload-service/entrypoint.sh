#!/usr/bin/env bash
# entrypoint.sh - load Docker secrets into environment and start the Spring Boot jar
# It looks for mounted secrets at /run/secrets/<name> and exports:
#   SECURITY_JWT_ACCESS_SECRET, SIGNEDURL_SECRET
set -e

# Helper to load a secret file to an env var if present
load_secret() {
  local secret_name="$1"
  local env_var="$2"
  local path="/run/secrets/${secret_name}"
  if [ -f "${path}" ]; then
    export "${env_var}"="$(cat "${path}")"
    echo "Loaded secret ${secret_name} into ${env_var}"
  else
    echo "Secret file ${path} not found, leaving ${env_var} unchanged"
  fi
}

# Load Docker secrets (names must match those defined in compose)
load_secret "jwt_access_secret" "SECURITY_JWT_ACCESS_SECRET"
load_secret "signedurl_secret" "SIGNEDURL_SECRET"

# Optionally support reading older env var names or fallback values here.
# Finally exec the jar (allow passing additional args)
exec java ${JAVA_OPTS:-} -jar /app/upload.jar "$@"