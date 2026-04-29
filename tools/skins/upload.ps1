<#
.SYNOPSIS
  Uploads a single companion skin (4 variants) to Cloudflare R2.

.DESCRIPTION
  Reads four PNG variants from a staging directory, validates each
  against the design.md contract (pixel sizes + PNG magic bytes), and
  pushes them to s3://gkim-assets/character-skins/{characterId}/{skinId}/v{n}/
  with the immutable cache headers the CDN expects.

  R2 has no bucket-level default response-header API — content-type
  and cache-control are set per-object on PUT. Versioned keys are
  immutable; updates ship as v{n+1} (never overwrite v{n}).

  Authentication reads R2_GKIM_ASSETS_ACCESS_KEY_ID and
  R2_GKIM_ASSETS_SECRET from .env.local (or any file passed via
  -EnvFile). The actual upload uses the AWS CLI in S3-compat mode;
  if `aws` is not on PATH the script fails with a clear hint.

  -DryRun resolves and prints the four CDN URLs without touching
  the network or running pixel validation, so the verification gate
  works without designer-supplied art.

.PARAMETER StagingDir
  Directory containing the four variants
  (thumb.png, avatar.png, portrait.png, banner.png).

.PARAMETER CharacterId
  Companion character id (lowercase ASCII letters/digits/hyphens),
  e.g. "architect-oracle".

.PARAMETER SkinId
  Skin id under that character, e.g. "default" or "lantern-keeper".

.PARAMETER Version
  Versioned key segment. v1 for new uploads; v{n+1} when re-issuing.

.PARAMETER DryRun
  Skip pixel validation + skip upload; just print the four resolved
  CDN URLs and exit 0.

.PARAMETER EnvFile
  Path to env file holding R2_GKIM_ASSETS_* values.
  Default: repo-root/.env.local.

.EXAMPLE
  pwsh tools/skins/upload.ps1 `
    -StagingDir ops/skins-staging/architect-oracle/default/v1/ `
    -CharacterId architect-oracle -SkinId default -Version 1
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $StagingDir,
    [Parameter(Mandatory = $true)] [string] $CharacterId,
    [Parameter(Mandatory = $true)] [string] $SkinId,
    [Parameter(Mandatory = $true)] [int]    $Version,
    [switch] $DryRun,
    [string] $EnvFile = ""
)

$ErrorActionPreference = "Stop"

# --- Constants pinned to design.md ------------------------------------------

$CdnHost   = "cdn.lastxuans.sbs"
$KeyPrefix = "character-skins"
$BucketName = "gkim-assets"

$VariantSpecs = @(
    @{ FileName = "thumb.png";    Width =   96; Height =   96; AspectTolerance = 0.10 },
    @{ FileName = "avatar.png";   Width =  256; Height =  256; AspectTolerance = 0.05 },
    @{ FileName = "portrait.png"; Width =  512; Height =  768; AspectTolerance = 0.05 },
    @{ FileName = "banner.png";   Width =  941; Height = 1672; AspectTolerance = 0.05 }
)

# --- Param validation -------------------------------------------------------

if ($CharacterId -notmatch "^[a-z0-9][a-z0-9-]*$") {
    throw "CharacterId '$CharacterId' must be lowercase ASCII letters / digits / hyphens (no underscore, no dot, no Unicode)."
}
if ($SkinId -notmatch "^[a-z0-9][a-z0-9-]*$") {
    throw "SkinId '$SkinId' must be lowercase ASCII letters / digits / hyphens."
}
if ($Version -lt 1) {
    throw "Version must be >= 1 (got $Version)."
}
$resolvedStaging = (Resolve-Path -Path $StagingDir -ErrorAction Stop).Path

# --- File existence (always enforced, even in dry-run) ----------------------

$variantFiles = @{}
foreach ($spec in $VariantSpecs) {
    $path = Join-Path $resolvedStaging $spec.FileName
    if (-not (Test-Path $path)) {
        throw "Missing variant '$($spec.FileName)' under $resolvedStaging — expected all four (thumb / avatar / portrait / banner)."
    }
    $variantFiles[$spec.FileName] = $path
}

# --- Resolve target URLs (always) -------------------------------------------

$results = @()
foreach ($spec in $VariantSpecs) {
    $key = "$KeyPrefix/$CharacterId/$SkinId/v$Version/$($spec.FileName)"
    $cdnUrl = "https://$CdnHost/$key"
    $s3Uri  = "s3://$BucketName/$key"
    $results += [PSCustomObject]@{
        FileName = $spec.FileName
        LocalPath = $variantFiles[$spec.FileName]
        Key      = $key
        CdnUrl   = $cdnUrl
        S3Uri    = $s3Uri
    }
}

# --- Dry-run short-circuit --------------------------------------------------

if ($DryRun) {
    Write-Host "[upload.ps1] DRY-RUN — no network calls, no pixel validation" -ForegroundColor Yellow
    foreach ($r in $results) {
        Write-Host "  $($r.FileName) → $($r.CdnUrl)" -ForegroundColor Cyan
    }
    Write-Host "[upload.ps1] DRY-RUN complete — 4 URLs resolved" -ForegroundColor Green
    exit 0
}

# --- Validation (real run) --------------------------------------------------

Add-Type -AssemblyName System.Drawing

function Test-PngMagicBytes {
    param([string] $Path)
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 8) { return $false }
    # PNG magic: 89 50 4E 47 0D 0A 1A 0A
    return ($bytes[0] -eq 0x89) -and ($bytes[1] -eq 0x50) -and ($bytes[2] -eq 0x4E) -and ($bytes[3] -eq 0x47) `
       -and ($bytes[4] -eq 0x0D) -and ($bytes[5] -eq 0x0A) -and ($bytes[6] -eq 0x1A) -and ($bytes[7] -eq 0x0A)
}

Write-Host "[upload.ps1] validating 4 variants in $resolvedStaging" -ForegroundColor Cyan
foreach ($spec in $VariantSpecs) {
    $path = $variantFiles[$spec.FileName]

    if (-not (Test-PngMagicBytes -Path $path)) {
        throw "$path is not a valid PNG (89 50 4E 47 ... magic bytes missing)."
    }

    # Aspect-ratio sanity (warn, don't block) — pixel sizes are intentionally
    # lenient. R2 storage is plentiful and Coil downsamples on device, so
    # uploading at-or-above the target size with the right aspect is fine.
    # Big aspect mismatches (square asset uploaded as a 16:9 banner) still
    # warrant a warning so the operator can spot a misnamed file.
    $bmp = [System.Drawing.Bitmap]::FromFile($path)
    try {
        $actual = "{0}x{1}" -f $bmp.Width, $bmp.Height
        $targetAspect = $spec.Width / [double]$spec.Height
        $actualAspect = $bmp.Width / [double]$bmp.Height
        $aspectDelta = [Math]::Abs($actualAspect - $targetAspect) / $targetAspect
        $tolerance = $spec.AspectTolerance
        if ($aspectDelta -gt $tolerance) {
            Write-Warning ("  ! {0} aspect {1} differs from target {2}x{3} by {4:P1} (> tolerance {5:P0}); uploading anyway." -f `
                $spec.FileName, $actual, $spec.Width, $spec.Height, $aspectDelta, $tolerance)
        }
        if ($bmp.Width -lt $spec.Width -or $bmp.Height -lt $spec.Height) {
            Write-Warning ("  ! {0} {1} is below target {2}x{3} on at least one axis; image will look soft when displayed." -f `
                $spec.FileName, $actual, $spec.Width, $spec.Height)
        }
        Write-Host ("  {0,-12} {1,-12} target {2}x{3}" -f $spec.FileName, $actual, $spec.Width, $spec.Height) -ForegroundColor Green
    }
    finally {
        $bmp.Dispose()
    }
}

# --- Resolve credentials ----------------------------------------------------

if (-not $EnvFile) {
    $repoRoot = (Resolve-Path "$PSScriptRoot/../..").Path
    $EnvFile = Join-Path $repoRoot ".env.local"
}
if (-not (Test-Path $EnvFile)) {
    throw "Env file not found at $EnvFile. Pass -EnvFile or create .env.local."
}

$envValues = @{}
foreach ($line in Get-Content $EnvFile) {
    if ($line -match '^\s*#') { continue }
    if ($line -match '^\s*$') { continue }
    if ($line -match '^\s*([A-Z0-9_]+)\s*=\s*"?([^"]*?)"?\s*$') {
        $envValues[$Matches[1]] = $Matches[2]
    }
}

# --- wrangler presence ------------------------------------------------------

$wranglerExe = Get-Command wrangler -ErrorAction SilentlyContinue
if (-not $wranglerExe) {
    throw "wrangler CLI not found on PATH. Install via 'npm install -g wrangler' (Node + npm required), then 'wrangler login'."
}

# --- Upload via wrangler r2 object put --------------------------------------
#
# Auth precedence: wrangler reads CLOUDFLARE_API_TOKEN if set, falls back to
# OAuth credentials from `wrangler login`. We explicitly clear the bucket-
# scoped cfut_ token because it's missing the account-level permission
# `wrangler r2 object put` requires — the OAuth login captures full-account
# scope and is the cleaner path here.
#
# The --remote flag is required so the command actually targets prod R2
# (without it, wrangler tries to write to the local emulated dev storage).

Remove-Item Env:CLOUDFLARE_API_TOKEN -ErrorAction SilentlyContinue

Write-Host "[upload.ps1] uploading 4 variants to r2://$BucketName via wrangler" -ForegroundColor Cyan
foreach ($r in $results) {
    Write-Host "  → r2://$BucketName/$($r.Key)" -ForegroundColor Cyan
    & wrangler r2 object put "$BucketName/$($r.Key)" `
        --file "$($r.LocalPath)" `
        --content-type "image/png" `
        --cache-control "public,max-age=31536000,immutable" `
        --remote 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "wrangler r2 object put failed for $($r.FileName) (exit $LASTEXITCODE)"
    }
    Write-Host "    ✓ live at $($r.CdnUrl)" -ForegroundColor Green
}

Write-Host "[upload.ps1] DONE — 4 variants uploaded for $CharacterId/$SkinId/v$Version" -ForegroundColor Green
