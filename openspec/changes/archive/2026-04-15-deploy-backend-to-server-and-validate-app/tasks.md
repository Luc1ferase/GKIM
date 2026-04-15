## 1. Backend remote deployment workflow

- [x] 1.1 Tighten the Ubuntu deployment/debug workflow under `backend/` so operators can sync the accepted backend slice to `/opt/gkim-im/backend`, manage `/etc/gkim-im-backend/gkim-im-backend.env`, restart `gkim-im-backend.service`, and distinguish service-status failures from published-endpoint failures.
- [x] 1.2 Deploy or redeploy the accepted backend slice to `124.222.15.128`, verify `gkim-im-backend.service` plus `/health`, and confirm the canonical remote HTTP/WebSocket endpoints are reachable for Android validation without relying on adb reverse or tunnel-only assumptions.

## 2. Android remote-backend validation path

- [x] 2.1 Update the Android IM validation configuration and endpoint-resolution path so the installed app can target the published remote backend HTTP/WebSocket endpoints cleanly without rebuilding or silently falling back to local-only defaults.
- [x] 2.2 Add or refresh focused Android validation coverage and diagnostics for remote endpoint selection, bootstrap/auth reachability, realtime connection setup, and explicit failure surfacing when the selected remote backend is unavailable or misconfigured.

## 3. End-to-end validation and evidence

- [x] 3.1 Launch the Android app against the deployed backend and run the live validation flow for session issuance, bootstrap/history hydration, realtime send/receive, and recovery after reconnect or relaunch.
- [x] 3.2 Record the verification, review, score, and upload evidence for the remote deployment and app-validation flow in `docs/DELIVERY_WORKFLOW.md`, and update any affected `backend/README.md` or `android/README.md` guidance before closing the change.
