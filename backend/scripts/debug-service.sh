#!/usr/bin/env bash
set -euo pipefail

SERVICE_NAME="${SERVICE_NAME:-gkim-im-backend}"
LINES="${LINES:-120}"

sudo systemctl --no-pager --full status "$SERVICE_NAME"
echo
sudo journalctl -u "$SERVICE_NAME" -n "$LINES" --no-pager
