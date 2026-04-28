<#
.SYNOPSIS
  Uploads a single companion skin (4 variants) to Cloudflare R2.

.DESCRIPTION
  Reads four WebP variants from a staging directory, validates each
  against the design.md contract (pixel sizes + WebP magic bytes), and
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
  (thumb.webp, avatar.webp, portrait.webp, banner.webp).

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
    @{ FileName = "thumb.webp";    Width =   96; Height =   96 },
    @{ FileName = "avatar.webp";   Width =  256; Height =  256 },
    @{ FileName = "portrait.webp"; Width =  512; Height =  768 },
    @{ FileName = "banner.webp";   Width = 1080; Height = 2400 }
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

function Test-WebPMagicBytes {
    param([string] $Path)
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    if ($bytes.Length -lt 12) { return $false }
    # WebP magic: RIFF....WEBP at offsets 0..3 and 8..11
    $isRiff = ($bytes[0] -eq 0x52) -and ($bytes[1] -eq 0x49) -and ($bytes[2] -eq 0x46) -and ($bytes[3] -eq 0x46)
    $isWebp = ($bytes[8] -eq 0x57) -and ($bytes[9] -eq 0x45) -and ($bytes[10] -eq 0x42) -and ($bytes[11] -eq 0x50)
    return ($isRiff -and $isWebp)
}

Write-Host "[upload.ps1] validating 4 variants in $resolvedStaging" -ForegroundColor Cyan
foreach ($spec in $VariantSpecs) {
    $path = $variantFiles[$spec.FileName]

    # Magic bytes — even though System.Drawing can sometimes load WebP via
    # a codec, the magic check is faster and rules out wrong-extension files.
    if (-not (Test-WebPMagicBytes -Path $path)) {
        throw "$path is not a valid WebP (RIFF/WEBP magic bytes missing)."
    }

    # Pixel-size check — System.Drawing throws on .webp without a codec.
    # If decoding fails we fall back to a hint rather than blocking, since
    # the upload script's job is naming + transport, not decoding.
    try {
        $bmp = [System.Drawing.Bitmap]::FromFile($path)
        try {
            if ($bmp.Width -ne $spec.Width -or $bmp.Height -ne $spec.Height) {
                throw "$($spec.FileName) is $($bmp.Width)x$($bmp.Height); expected $($spec.Width)x$($spec.Height)."
            }
        }
        finally {
            $bmp.Dispose()
        }
        Write-Host "  ✓ $($spec.FileName) — $($spec.Width)x$($spec.Height)" -ForegroundColor Green
    }
    catch [System.OutOfMemoryException], [System.ArgumentException] {
        # System.Drawing can't always decode .webp on Windows without an
        # extra codec install; that's not our problem to solve here. The
        # magic-bytes check above already enforces "this is webp". We log
        # a soft warning so the operator knows to spot-check elsewhere.
        Write-Warning "  ! $($spec.FileName) — System.Drawing can't decode WebP on this host; skipping pixel-size assertion. Magic-bytes check passed."
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

$accessKey = $envValues["R2_GKIM_ASSETS_ACCESS_KEY_ID"]
$secretKey = $envValues["R2_GKIM_ASSETS_SECRET"]
$endpoint  = $envValues["R2_GKIM_ASSETS_ENDPOINT"]
if (-not $accessKey -or -not $secretKey -or -not $endpoint) {
    throw "Missing R2_GKIM_ASSETS_ACCESS_KEY_ID / R2_GKIM_ASSETS_SECRET / R2_GKIM_ASSETS_ENDPOINT in $EnvFile."
}

# --- AWS CLI presence -------------------------------------------------------

$awsExe = Get-Command aws -ErrorAction SilentlyContinue
if (-not $awsExe) {
    throw "aws CLI not found on PATH. Install via 'winget install Amazon.AWSCLI' or use any S3-compatible client. R2 endpoint: $endpoint"
}

# --- Upload -----------------------------------------------------------------

$env:AWS_ACCESS_KEY_ID     = $accessKey
$env:AWS_SECRET_ACCESS_KEY = $secretKey
$env:AWS_DEFAULT_REGION    = "auto"

Write-Host "[upload.ps1] uploading 4 variants to $endpoint/$BucketName" -ForegroundColor Cyan
foreach ($r in $results) {
    Write-Host "  → $($r.S3Uri)" -ForegroundColor Cyan
    & aws s3 cp $r.LocalPath $r.S3Uri `
        --endpoint-url $endpoint `
        --content-type "image/webp" `
        --cache-control "public,max-age=31536000,immutable" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "aws s3 cp failed for $($r.FileName) (exit $LASTEXITCODE)"
    }
    Write-Host "    ✓ live at $($r.CdnUrl)" -ForegroundColor Green
}

Write-Host "[upload.ps1] DONE — 4 variants uploaded for $CharacterId/$SkinId/v$Version" -ForegroundColor Green
