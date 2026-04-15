#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://127.0.0.1:8080}"
DEV_USER_EXTERNAL_ID="${DEV_USER_EXTERNAL_ID:-nox-dev}"
PYTHON_BIN="${PYTHON_BIN:-python3}"

if ! command -v curl >/dev/null 2>&1; then
  echo "curl is required for session smoke checks." >&2
  exit 1
fi

if ! command -v "$PYTHON_BIN" >/dev/null 2>&1; then
  echo "$PYTHON_BIN is required for session smoke checks." >&2
  exit 1
fi

BACKEND_URL="${BACKEND_URL%/}"
SESSION_URL="$BACKEND_URL/api/session/dev"
BOOTSTRAP_URL="$BACKEND_URL/api/bootstrap"

SESSION_PAYLOAD="$("$PYTHON_BIN" - "$DEV_USER_EXTERNAL_ID" <<'PY'
import json
import sys

print(json.dumps({"externalId": sys.argv[1]}))
PY
)"

SESSION_RESPONSE="$(
  curl \
    --fail \
    --silent \
    --show-error \
    -H "Content-Type: application/json" \
    -d "$SESSION_PAYLOAD" \
    "$SESSION_URL"
)"

TOKEN="$(
  printf '%s' "$SESSION_RESPONSE" | "$PYTHON_BIN" -c 'import json, sys; print(json.load(sys.stdin).get("token", ""))'
)"

if [[ -z "$TOKEN" ]]; then
  echo "Session smoke check did not return a bearer token." >&2
  exit 1
fi

BOOTSTRAP_RESPONSE="$(
  curl \
    --fail \
    --silent \
    --show-error \
    -H "Authorization: Bearer $TOKEN" \
    "$BOOTSTRAP_URL"
)"

printf '%s' "$BOOTSTRAP_RESPONSE" | "$PYTHON_BIN" -c '
import json
import sys

bootstrap = json.load(sys.stdin)
user = bootstrap.get("user", {}).get("externalId", "<unknown>")
contacts = len(bootstrap.get("contacts", []))
conversations = len(bootstrap.get("conversations", []))
print(f"user={user} contacts={contacts} conversations={conversations}")
'
