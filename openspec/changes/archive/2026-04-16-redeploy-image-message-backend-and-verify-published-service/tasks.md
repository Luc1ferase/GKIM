## 1. Remote rollout

- [x] 1.1 Sync the accepted `backend/` slice to the Ubuntu deployment directory and rerun the existing bootstrap/systemd flow so `gkim-im-backend.service` is rebuilt and restarted on the current host behind `chat.lastxuans.sbs`.
- [x] 1.2 Confirm the remote deployment is actually serving the new backend version by checking the deployed files or runtime process state for the image-message API additions after restart.

## 2. Service verification

- [x] 2.1 Run host-local backend smoke checks on the server for `/health`, session/bootstrap, direct image upload, and attachment fetch.
- [x] 2.2 Run the same image-message-capable API checks through the published `chat.lastxuans.sbs` endpoint and confirm the published service no longer returns `404` for image send.

## 3. Evidence and guidance

- [x] 3.1 Record the deployment and verification evidence in `docs/DELIVERY_WORKFLOW.md`, including enough detail to distinguish host-local success from published-endpoint success.
- [x] 3.2 Update any affected backend operator guidance so future deployments explicitly verify the image-message API version rather than relying on generic health-only checks.
