#!/usr/bin/env bash
# entrypoint.sh for streaming-service
# Loads secrets from /run/secrets/<name> into environment variables that the app expects.
# Currently loads:
#   signedurl_secret  -> SIGNEDURL_SECRET
#   jwt_access_secret -> SECURITY_JWT_ACCESS_SECRET (optional)
set -e

load_secret_to_env() {
  secret_name="$1"
  env_var="$2"
  secret_path="/run/secrets/${secret_name}"
  if [ -f "${secret_path}" ]; then
    value="$(tr -d '\r' < "${secret_path}")"
    export "${env_var}"="${value}"
    echo "Loaded secret ${secret_name} -> ${env_var}"
  else
    echo "Secret ${secret_name} not found at ${secret_path}; ${env_var} unchanged"
  fi
}

# Load secrets declared in compose/stack
load_secret_to_env "signedurl_secret"  "SIGNEDURL_SECRET"
load_secret_to_env "jwt_access_secret" "SECURITY_JWT_ACCESS_SECRET"

# Exec the Java process
exec java ${JAVA_OPTS:-} -jar /app/streaming.jar "$@"