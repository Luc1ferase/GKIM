## Why

The image-message backend code now exists locally, but the published service still needs a deployment-focused acceptance slice that proves the Ubuntu host is actually running the new version instead of an older build that returns `404` for the image-send API. We need a dedicated operations change now so backend rollout, published-endpoint verification, and evidence capture are treated as first-class deliverables rather than ad-hoc shell work.

## What Changes

- Redeploy the Rust IM backend to the current Ubuntu host behind `chat.lastxuans.sbs` using the existing secret-managed systemd workflow, but explicitly target the backend build that includes the direct image-message API and attachment fetch support.
- Tighten backend deployment verification so host-local and published-endpoint smoke checks prove `health`, session/bootstrap, direct image upload, and attachment fetch all work after rollout.
- Refresh repository guidance and delivery evidence for the deployed-service workflow, including how operators distinguish an outdated backend build from DNS, proxy, or Android-client issues.
- Capture the accepted deployment state for the current published backend instead of assuming a successful Android tag release also updated server-side IM behavior.

## Capabilities

### New Capabilities
- None.

### Modified Capabilities
- `im-backend`: update the remote deployment/debug requirement so deployed-service acceptance must verify that the published backend is running the image-message-capable version, not only that generic health and bootstrap checks pass.

## Impact

- Affected code and assets: `backend/scripts/**`, `backend/README.md`, `docs/DELIVERY_WORKFLOW.md`, and any deployment/supporting backend files needed to redeploy the current Rust IM service.
- Affected systems: the Ubuntu host serving `chat.lastxuans.sbs`, `gkim-im-backend.service`, the published HTTP/WebSocket endpoints, and the operational flow that promotes backend changes into production validation.
- Affected APIs: `POST /api/direct-messages/image`, attachment fetch under `/api/messages/{id}/attachment`, plus the existing health/session/bootstrap checks used to verify the deployed service.
