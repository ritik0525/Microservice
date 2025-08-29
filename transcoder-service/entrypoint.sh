#!/usr/bin/env bash
# entrypoint.sh for transcoder-service
# Loads secrets from /run/secrets/<name> into environment variables that the app expects.
# Currently loads:
#   jwt_access_secret -> SECURITY_JWT_ACCESS_SECRET
#   signedurl_secret  -> SIGNEDURL_SECRET
# Then execs the jar.
set -e

load_secret_to_env() {
  secret_name="$1"
  env_var="$2"
  secret_path="/run/secrets/${secret_name}"
  if [ -f "${secret_path}" ]; then
    # strip CRLF
    value="$(tr -d '\r' < "${secret_path}")"
    export "${env_var}"="${value}"
    echo "Loaded secret ${secret_name} -> ${env_var}"
  else
    echo "Secret ${secret_name} not found at ${secret_path}; ${env_var} unchanged"
  fi
}

# Load commonly used secrets (adjust names if your compose/k8s uses different names)
load_secret_to_env "jwt_access_secret" "SECURITY_JWT_ACCESS_SECRET"
load_secret_to_env "signedurl_secret"  "SIGNEDURL_SECRET"

# Launch the java process (allow overriding via JAVA_OPTS)
exec java ${JAVA_OPTS:-} -jar /app/transcoder.jar "$@"