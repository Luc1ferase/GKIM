#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8080}"
HEALTH_URL="${BACKEND_URL%/}/health"

curl --fail --silent --show-error "$HEALTH_URL"
echo
