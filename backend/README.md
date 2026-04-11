# GKIM IM Backend Ops Notes

## Secrets

- Keep PostgreSQL passwords and any live DSN values in an untracked `backend/.env.local`
  file for local work, or in `/etc/gkim-im-backend/gkim-im-backend.env` on Ubuntu.
- Do not commit the SSH password, PostgreSQL password, or a filled `.env` file.
- Only set `PGSSLROOTCERT` if operations confirms the replacement PostgreSQL server needs
  custom trust material.

## Expected Ubuntu layout

- Backend checkout: `/opt/gkim-im/backend`
- Runtime env file: `/etc/gkim-im-backend/gkim-im-backend.env`
- System service: `gkim-im-backend.service`

## Local smoke test

1. Copy `backend/.env.example` to `backend/.env.local`.
2. Fill in the local secret values without committing that file.
3. Start the service with your shell env loaded and call `/health`.
4. The backend now auto-applies checked-in SQL migrations on startup, including legacy bootstrap-only
   databases that predate the auth migration ledger.

Example PowerShell:

```powershell
Get-Content .env.local | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $parts = $_ -split '=', 2
  Set-Item -Path ("Env:" + $parts[0]) -Value $parts[1]
}
cargo run
```

## Local Docker image

Build the backend image from `backend/`:

```powershell
docker build -t gkim-im-backend:local .
```

Run the container with local env values and publish the service on host port `18080`:

```powershell
docker run --rm -d `
  --name gkim-im-backend-local `
  --env-file .env.local `
  -e APP_BIND_ADDR=0.0.0.0:8080 `
  -p 18080:8080 `
  gkim-im-backend:local
```

Then verify the published health endpoint from the host:

```powershell
Invoke-WebRequest http://127.0.0.1:18080/health | Select-Object -ExpandProperty Content
```

When validating from the Android emulator, do not point the client at `127.0.0.1:18080`.
Inside the emulator that loopback address resolves to the device itself, not to the host-published
backend. Use the host bridge endpoints instead:

```text
HTTP: http://10.0.2.2:18080/
WS:   ws://10.0.2.2:18080/ws
```

This local image is also intended to become the deployable server image later, so keep the runtime env contract based on `.env.local` / deployment secrets rather than baking secrets into the image.
The image must continue shipping the `migrations/` directory because startup now reconciles the
runtime schema before serving requests.

## Ubuntu bootstrap and debug

1. SSH to `ubuntu@124.222.15.128`.
2. Sync this `backend/` directory to `/opt/gkim-im/backend`.
3. Edit `/etc/gkim-im-backend/gkim-im-backend.env` with real secret values.
4. Run `./scripts/bootstrap-ubuntu.sh` from `/opt/gkim-im/backend`.
5. Run `./scripts/smoke-health.sh` to confirm `200 OK`.
6. Use `./scripts/debug-service.sh` or `sudo journalctl -u gkim-im-backend -f` for logs.

The repo ships only placeholder values and service scaffolding. SSH auth remains interactive
or key-based outside version control.
