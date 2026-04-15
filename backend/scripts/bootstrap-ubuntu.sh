#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="${SERVICE_NAME:-gkim-im-backend}"
SERVICE_GROUP="${SERVICE_GROUP:-ubuntu}"
BACKEND_ROOT="${BACKEND_ROOT:-/opt/gkim-im/backend}"
ENV_DIR="${ENV_DIR:-/etc/gkim-im-backend}"
ENV_FILE="${ENV_FILE:-$ENV_DIR/gkim-im-backend.env}"
UNIT_SOURCE="$BACKEND_ROOT/systemd/${SERVICE_NAME}.service"
UNIT_TARGET="/etc/systemd/system/${SERVICE_NAME}.service"
ENV_TEMPLATE="$BACKEND_ROOT/.env.example"

if [[ ! -f "$BACKEND_ROOT/Cargo.toml" ]]; then
  echo "Expected a backend checkout at $BACKEND_ROOT" >&2
  exit 1
fi

if [[ ! -f "$UNIT_SOURCE" ]]; then
  echo "Missing systemd unit template at $UNIT_SOURCE" >&2
  exit 1
fi

if ! command -v cargo >/dev/null 2>&1 && [[ -x "$HOME/.cargo/bin/cargo" ]]; then
  export PATH="$HOME/.cargo/bin:$PATH"
fi

if ! command -v cargo >/dev/null 2>&1; then
  echo "cargo is required on the Ubuntu host before bootstrapping." >&2
  exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
  echo "systemctl is required on the Ubuntu host before bootstrapping." >&2
  exit 1
fi

sudo install -d -m 0755 "$ENV_DIR"

if [[ ! -f "$ENV_FILE" ]]; then
  sudo install -m 0640 -o root -g "$SERVICE_GROUP" "$ENV_TEMPLATE" "$ENV_FILE"
  echo "Created placeholder env file at $ENV_FILE"
fi

sudo install -m 0644 "$UNIT_SOURCE" "$UNIT_TARGET"

cd "$BACKEND_ROOT"
cargo build --release

sudo systemctl daemon-reload
sudo systemctl enable "$SERVICE_NAME"

if grep -q "<set" "$ENV_FILE"; then
  echo "Placeholder secret values remain in $ENV_FILE. Edit the file before starting $SERVICE_NAME."
  exit 0
fi

sudo systemctl restart "$SERVICE_NAME"
sudo systemctl --no-pager --full status "$SERVICE_NAME"
