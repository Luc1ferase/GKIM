<#
.SYNOPSIS
  Generate one companion skin master via the OpenAI-compatible image API,
  then center-crop into the four variants the catalog expects.

.DESCRIPTION
  R3.1 helper. Calls POST /v1/images/generations on the configured gateway
  with the per-skin prompt, decodes the base64 master to a 1024×1024 PNG,
  and writes thumb / avatar / portrait / banner crops under
  ops/skins-staging/{characterId}/{skinId}/v1/{variant}.png.

  Gateway: api.lastxuans.sbs caps gpt-image-2 requests at ~60s, which only
  square 1024×1024 can sneak under reliably. Tall-aspect variants are
  derived by center-cropping the square master:
    banner    9:16  ← center 576×1024 strip from the square
    portrait  2:3   ← center 683×1024 strip from the square
    avatar    1:1   ← top-center 256×256 (head + shoulders region)
    thumb     1:1   ← upper-third 96×96 (face area)

.PARAMETER CharacterId
  Companion character id (lowercase ASCII letters / digits / hyphens).

.PARAMETER SkinId
  Skin id under that character (e.g. "lantern-keeper", "lavender-keeper").

.PARAMETER Version
  Versioned key segment. v1 for new uploads.

.PARAMETER PromptFile
  Path to a UTF-8 text file containing the full image prompt.

.PARAMETER ApiBase
  Base URL of the OpenAI-compat image gateway. Default reads
  $env:GKIM_IMAGE_API_BASE; falls back to https://api.lastxuans.sbs/.

.PARAMETER ApiKey
  Auth token. Default reads $env:GKIM_IMAGE_API_KEY.

.PARAMETER Model
  Image model name. Default reads $env:GKIM_IMAGE_API_MODEL; falls back
  to "gpt-image-2".

.EXAMPLE
  pwsh tools/skins/generate.ps1 `
    -CharacterId tavern-keeper -SkinId lantern-keeper -Version 1 `
    -PromptFile tools/skins/prompts/tavern-keeper-lantern-keeper.txt
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $CharacterId,
    [Parameter(Mandatory = $true)] [string] $SkinId,
    [Parameter(Mandatory = $true)] [int]    $Version = 1,
    [Parameter(Mandatory = $true)] [string] $PromptFile,
    [string] $ApiBase = $env:GKIM_IMAGE_API_BASE,
    [string] $ApiKey  = $env:GKIM_IMAGE_API_KEY,
    [string] $Model   = $env:GKIM_IMAGE_API_MODEL
)

$ErrorActionPreference = "Stop"

if (-not $ApiBase) { $ApiBase = "https://api.lastxuans.sbs/" }
if (-not $Model)   { $Model   = "gpt-image-2" }
if (-not $ApiKey)  {
    throw "GKIM_IMAGE_API_KEY env var or -ApiKey param required."
}
if ($CharacterId -notmatch "^[a-z0-9][a-z0-9-]*$") {
    throw "CharacterId '$CharacterId' must be lowercase ASCII letters / digits / hyphens."
}
if ($SkinId -notmatch "^[a-z0-9][a-z0-9-]*$") {
    throw "SkinId '$SkinId' must be lowercase ASCII letters / digits / hyphens."
}
if (-not (Test-Path $PromptFile)) {
    throw "Prompt file not found: $PromptFile"
}

$prompt = (Get-Content -Path $PromptFile -Raw -Encoding UTF8).Trim()
if (-not $prompt) {
    throw "Prompt file is empty: $PromptFile"
}

# --- Output paths -----------------------------------------------------------

$repoRoot = (Resolve-Path "$PSScriptRoot/../..").Path
$stagingDir = Join-Path $repoRoot "ops/skins-staging/$CharacterId/$SkinId/v$Version"
New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null

$masterPath   = Join-Path $stagingDir "master-1024.png"
$bannerPath   = Join-Path $stagingDir "banner.png"
$portraitPath = Join-Path $stagingDir "portrait.png"
$avatarPath   = Join-Path $stagingDir "avatar.png"
$thumbPath    = Join-Path $stagingDir "thumb.png"

# --- Generate master --------------------------------------------------------

$body = @{
    model  = $Model
    prompt = $prompt
    size   = "1024x1024"
    n      = 1
} | ConvertTo-Json -Depth 4 -Compress

$endpoint = ($ApiBase.TrimEnd("/")) + "/v1/images/generations"
Write-Host "[generate] POST $endpoint  (model=$Model, size=1024x1024)" -ForegroundColor Cyan
Write-Host "[generate] writing master to $masterPath" -ForegroundColor Cyan

$resp = Invoke-RestMethod -Method Post -Uri $endpoint `
    -Headers @{ "Authorization" = "Bearer $ApiKey"; "Content-Type" = "application/json" } `
    -Body $body -TimeoutSec 600

if (-not $resp.data -or $resp.data.Count -lt 1 -or -not $resp.data[0].b64_json) {
    throw "Unexpected response — no b64_json payload."
}

$bytes = [Convert]::FromBase64String($resp.data[0].b64_json)
[System.IO.File]::WriteAllBytes($masterPath, $bytes)
Write-Host ("[generate] master saved ({0:N0} bytes)" -f $bytes.Length) -ForegroundColor Green

# --- Crop variants ----------------------------------------------------------

Add-Type -AssemblyName System.Drawing

function Save-Crop {
    param(
        [System.Drawing.Bitmap] $Source,
        [int] $SourceX,
        [int] $SourceY,
        [int] $SourceW,
        [int] $SourceH,
        [int] $TargetW,
        [int] $TargetH,
        [string] $OutPath
    )
    $target = New-Object System.Drawing.Bitmap($TargetW, $TargetH)
    $g = [System.Drawing.Graphics]::FromImage($target)
    try {
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $srcRect = New-Object System.Drawing.Rectangle($SourceX, $SourceY, $SourceW, $SourceH)
        $dstRect = New-Object System.Drawing.Rectangle(0, 0, $TargetW, $TargetH)
        $g.DrawImage($Source, $dstRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    } finally {
        $g.Dispose()
    }
    $target.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $target.Dispose()
    $size = (Get-Item $OutPath).Length
    Write-Host ("  ✓ {0,-12} {1,-10} ({2:N0} bytes)" -f (Split-Path -Leaf $OutPath), "${TargetW}x${TargetH}", $size) -ForegroundColor Green
}

$src = [System.Drawing.Bitmap]::FromFile($masterPath)
try {
    $W = $src.Width
    $H = $src.Height

    # Banner 9:16 — center-cropped vertical strip, then upscaled to 941x1672.
    # 9/16 = 0.5625; from W=1024 we take 576 wide × 1024 tall starting at x=224.
    $bannerSrcW = [int]([math]::Round($H * 9.0 / 16.0))   # 576
    $bannerSrcX = [int](($W - $bannerSrcW) / 2)            # 224
    Save-Crop -Source $src -SourceX $bannerSrcX -SourceY 0 -SourceW $bannerSrcW -SourceH $H `
              -TargetW 941 -TargetH 1672 -OutPath $bannerPath

    # Portrait 2:3 — center-cropped vertical strip, downscaled to 512x768.
    # 2/3 = 0.6667; from W=1024 we take 683 wide × 1024 tall starting at x=170.
    $portraitSrcW = [int]([math]::Round($H * 2.0 / 3.0))   # 683
    $portraitSrcX = [int](($W - $portraitSrcW) / 2)         # 170
    Save-Crop -Source $src -SourceX $portraitSrcX -SourceY 0 -SourceW $portraitSrcW -SourceH $H `
              -TargetW 512 -TargetH 768 -OutPath $portraitPath

    # Avatar 1:1 — top-center 768×768 area (head + upper torso), downscaled to 256x256.
    $avatarSize = 768
    $avatarX = [int](($W - $avatarSize) / 2)
    Save-Crop -Source $src -SourceX $avatarX -SourceY 0 -SourceW $avatarSize -SourceH $avatarSize `
              -TargetW 256 -TargetH 256 -OutPath $avatarPath

    # Thumb 1:1 — face area, upper-third 384×384 region, downscaled to 96x96.
    $thumbSize = 384
    $thumbX = [int](($W - $thumbSize) / 2)
    $thumbY = [int]([math]::Round($H * 0.05))   # slight inset from very top
    Save-Crop -Source $src -SourceX $thumbX -SourceY $thumbY -SourceW $thumbSize -SourceH $thumbSize `
              -TargetW 96 -TargetH 96 -OutPath $thumbPath
} finally {
    $src.Dispose()
}

Write-Host "[generate] DONE — 4 variants ready under $stagingDir" -ForegroundColor Green
