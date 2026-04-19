## Context

The repository already contains the backend-side image-message implementation, deployment scripts, and a published backend endpoint at `chat.lastxuans.sbs`, but those pieces can drift apart operationally. A healthy `/health` response alone is not enough to prove that the live service is running the image-message-capable backend version: the published host can still serve an older binary that lacks `POST /api/direct-messages/image` and attachment fetch support. Recent debugging showed exactly that failure mode, where local verification passed while the published endpoint still returned `404` for the image-send path.

This change is operational rather than feature-implementation-heavy. The work must confirm that the current Ubuntu deployment target behind `chat.lastxuans.sbs` is updated to the right backend build, restarted cleanly, and validated through the same published interface the Android client uses. The design therefore focuses on repeatable rollout, layered smoke checks, and evidence capture rather than new product behavior.

## Goals / Non-Goals

**Goals:**
- Redeploy the backend on the current Ubuntu host behind `chat.lastxuans.sbs` so the published service includes the direct image-message API and attachment fetch behavior.
- Define a deployment acceptance chain that verifies host-local service health and the published image-message endpoints separately, so an outdated binary is distinguishable from DNS or reverse-proxy problems.
- Keep the rollout on the existing `/opt/gkim-im/backend` + `gkim-im-backend.service` systemd model instead of introducing another deployment mechanism.
- Record the resulting evidence in the maintained repository workflow/docs so future server-side rollouts can prove the same contract quickly.

**Non-Goals:**
- Re-implement image-message behavior in backend code; this change assumes the feature already exists in the accepted codebase.
- Redesign Android client endpoint handling or add new app behavior beyond what is necessary to verify the deployed backend.
- Replace the current systemd-based server shape with containers, orchestration, or a new CI/CD deployment stack.

## Decisions

### 1. Treat deployment acceptance as an API-version verification problem, not only a service-uptime problem

The deployment workflow will explicitly verify `POST /api/direct-messages/image` and attachment fetch on top of the existing `/health` and session/bootstrap checks.

Why:
- A running service can still be the wrong version.
- The user-visible failure here was not total downtime; it was an older backend lacking the new image API.
- Verifying the exact published API contract closes the gap between “service is up” and “feature is actually deployed.”

Alternatives considered:
- Keep only `/health` plus bootstrap checks: rejected because that would miss the exact regression this change exists to prevent.

### 2. Reuse the existing Ubuntu bootstrap/systemd flow as the deployment mechanism

Deployment will continue to use `/opt/gkim-im/backend`, `/etc/gkim-im-backend/gkim-im-backend.env`, and `gkim-im-backend.service`, with `backend/scripts/bootstrap-ubuntu.sh` as the restart/build entry point.

Why:
- The repository already documents and uses this flow.
- It limits change scope to verification and rollout correctness.
- It keeps secret handling on the server where it already belongs.

Alternatives considered:
- Introduce a new remote container or release-packaging flow: rejected because it would enlarge the change without helping prove the current published service version.

### 3. Verify host-local and published endpoints separately

Acceptance will first validate the service on `127.0.0.1:18080` on the server, then validate the same image-message contract through `https://chat.lastxuans.sbs`.

Why:
- If host-local checks pass and published checks fail, the problem is proxy/DNS/publication drift rather than the backend binary itself.
- If both fail, the deployment itself is still broken.
- This layered split speeds up debugging and matches the existing backend README guidance.

Alternatives considered:
- Verify only the published URL: rejected because it hides whether the failure is in the backend process or the path from the internet to that backend.

## Risks / Trade-offs

- [The server may be reachable but not actually serving the new backend binary] → Mitigation: require a post-deploy image-message API check, not just a health check.
- [Published endpoint failures may still stem from DNS, firewall, or reverse-proxy state outside the backend checkout] → Mitigation: preserve the host-local vs published smoke-check split in the acceptance workflow.
- [Operational fixes can be applied manually and then forgotten] → Mitigation: require the accepted deployment evidence and updated guidance in repository docs as part of the same change.

## Migration Plan

1. Sync the accepted backend checkout to the current Ubuntu deployment directory.
2. Run the existing bootstrap/systemd flow to rebuild and restart `gkim-im-backend.service`.
3. Verify host-local `/health`, session/bootstrap, direct image upload, and attachment fetch on the server.
4. Verify the same image-message contract through `https://chat.lastxuans.sbs`.
5. Record the deployment evidence and any updated operator guidance in the repository.

Rollback strategy:
- Restore the previous known-good backend contents under `/opt/gkim-im/backend` and rerun the same systemd bootstrap/restart flow if the published image-message contract regresses after rollout.

## Open Questions

- Should the long-term accepted published validation target remain `chat.lastxuans.sbs`, or should the backend docs also bless a second canonical endpoint for cases where emulator DNS resolution is unstable?
