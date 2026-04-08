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

Example PowerShell:

```powershell
Get-Content .env.local | ForEach-Object {
  if ($_ -match '^\s*#' -or $_ -notmatch '=') { return }
  $parts = $_ -split '=', 2
  Set-Item -Path ("Env:" + $parts[0]) -Value $parts[1]
}
cargo run
```

## Ubuntu bootstrap and debug

1. SSH to `ubuntu@124.222.15.128`.
2. Sync this `backend/` directory to `/opt/gkim-im/backend`.
3. Edit `/etc/gkim-im-backend/gkim-im-backend.env` with real secret values.
4. Run `./scripts/bootstrap-ubuntu.sh` from `/opt/gkim-im/backend`.
5. Run `./scripts/smoke-health.sh` to confirm `200 OK`.
6. Use `./scripts/debug-service.sh` or `sudo journalctl -u gkim-im-backend -f` for logs.

The repo ships only placeholder values and service scaffolding. SSH auth remains interactive
or key-based outside version control.
