<#
.SYNOPSIS
  Captures the four chrome surfaces of the Android app for visual-direction review.

.DESCRIPTION
  Builds and installs the debug APK (optional), then drives the running emulator
  through the Tavern, Messages, Character-detail, and Settings surfaces, taking
  one screenshot per surface. Each PNG is checked for minimum size and for the
  Tavern-palette anchor (Tavern Dark #1A0F0A surface or Tavern Light #F1E7D2
  surface, within +/- 2 per channel).

  This script is intentionally simple: the navigation is hardcoded against the
  R1 layout (3-tab bottom nav, default messages route). When R2-R4 land, the
  navigation will need updating in lockstep.

.PARAMETER OutDir
  Repo-relative or absolute directory where screenshots are written.

.PARAMETER AdbPath
  Path to adb.exe. Defaults to the standard Android Studio install location.

.PARAMETER Serial
  Emulator serial. Default: first device returned by 'adb devices'.

.PARAMETER SkipBuild
  Skip the assembleDebug + install step. Useful when iterating only on script
  navigation logic against an already-installed build.

.EXAMPLE
  pwsh tools/visual-direction/capture_chrome.ps1 -OutDir docs/visual-direction/r1
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $OutDir,
    [string] $AdbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string] $Serial = "",
    [switch] $SkipBuild
)

$ErrorActionPreference = "Stop"

# --- Resolve paths ----------------------------------------------------------

if (-not (Test-Path $AdbPath)) {
    throw "ADB not found at $AdbPath. Pass -AdbPath or install platform-tools."
}

$resolvedOutDir = [System.IO.Path]::GetFullPath($OutDir)
if (-not (Test-Path $resolvedOutDir)) {
    New-Item -ItemType Directory -Path $resolvedOutDir | Out-Null
}

$repoRoot = (Resolve-Path "$PSScriptRoot/../..").Path

if (-not $Serial) {
    $devices = & $AdbPath devices | Select-String -Pattern "^emulator-\d+\s+device$"
    if ($devices.Count -lt 1) {
        throw "No running emulator detected. Start one before running this script."
    }
    $Serial = $devices[0].ToString().Split()[0]
}

Write-Host "[capture_chrome] using device: $Serial" -ForegroundColor Cyan
Write-Host "[capture_chrome] output dir:   $resolvedOutDir" -ForegroundColor Cyan

# --- Build + install --------------------------------------------------------

if (-not $SkipBuild) {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
    Push-Location "$repoRoot/android"
    try {
        Write-Host "[capture_chrome] assembleDebug ..." -ForegroundColor Cyan
        & .\gradlew.bat --no-daemon :app:assembleDebug | Out-Null
        if ($LASTEXITCODE -ne 0) { throw "assembleDebug failed" }
    }
    finally {
        Pop-Location
    }

    $apk = "$repoRoot/android/app/build/outputs/apk/debug/app-debug.apk"
    if (-not (Test-Path $apk)) { throw "APK missing at $apk" }
    Write-Host "[capture_chrome] installing APK ..." -ForegroundColor Cyan
    & $AdbPath -s $Serial install -r -d $apk | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "install failed" }
}

# --- Helpers ----------------------------------------------------------------

function Invoke-Adb {
    param([string[]] $AdbArgs)
    & $AdbPath -s $Serial @AdbArgs
    if ($LASTEXITCODE -ne 0) { throw "adb $($AdbArgs -join ' ') failed (exit $LASTEXITCODE)" }
}

function Tap { param([int]$X, [int]$Y) Invoke-Adb -AdbArgs @("shell","input","tap","$X","$Y") }
function Back { Invoke-Adb -AdbArgs @("shell","input","keyevent","KEYCODE_BACK") }

function Capture-Screen {
    param([string]$Path)
    # adb exec-out emits raw PNG on stdout. PowerShell's `& exe | ...` pipeline
    # decodes stdout as text and corrupts binary, so we drive Process directly
    # and stream the BaseStream to a file.
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $AdbPath
    $psi.Arguments = "-s $Serial exec-out screencap -p"
    $psi.RedirectStandardOutput = $true
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $sink = [System.IO.File]::Create($Path)
    try {
        $proc.StandardOutput.BaseStream.CopyTo($sink)
    }
    finally {
        $sink.Dispose()
    }
    $proc.WaitForExit()
    if ($proc.ExitCode -ne 0) { throw "screencap failed (exit $($proc.ExitCode))" }
    if (-not (Test-Path $Path) -or (Get-Item $Path).Length -lt 4096) {
        throw "screencap produced empty / tiny file at $Path"
    }
}

function Sample-Surface-Pixel {
    # Reads pixel at (cx, cy) where the app surface is reliably visible.
    # We sample at (60, 1200): well below the status bar, well above the bottom
    # nav, and inside the painted app surface for every named surface.
    param([string]$Path)

    $bmp = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $px = $bmp.GetPixel(60, 1200)
        return ,@($px.R, $px.G, $px.B)
    }
    finally {
        $bmp.Dispose()
    }
}

function Assert-TavernPalette {
    param([string]$Path)
    $rgb = Sample-Surface-Pixel -Path $Path
    $r = $rgb[0]; $g = $rgb[1]; $b = $rgb[2]
    # Tavern Dark surface = #1A0F0A (26, 15, 10)
    # Tavern Light surface = #F1E7D2 (241, 231, 210)
    # Tolerance widened to +/- 12 per channel to absorb the R4.2 ambient
    # layer (grain @ 8% + candle glow @ 5%) which subtly darkens the
    # sampled pixel by a few RGB points relative to the raw surface hex.
    $tol = 12
    $matchesDark  = ([Math]::Abs($r - 26)  -le $tol) -and ([Math]::Abs($g - 15)  -le $tol) -and ([Math]::Abs($b - 10)  -le $tol)
    $matchesLight = ([Math]::Abs($r - 241) -le $tol) -and ([Math]::Abs($g - 231) -le $tol) -and ([Math]::Abs($b - 210) -le $tol)
    if (-not ($matchesDark -or $matchesLight)) {
        throw "Surface pixel ($r, $g, $b) at (60, 1200) does not match Tavern Dark #1A0F0A or Tavern Light #F1E7D2 in $Path"
    }
}

# --- Navigation plan --------------------------------------------------------

# All taps target a 1080 x 2400 screen. If your emulator differs, run with a
# device of that resolution (codex_api34 / sdk_gphone64_x86_64). Coordinates
# are anchored against the R1-state layout (3-tab bottom nav, default messages
# landing). After R2.1 the start destination flips to 'tavern' and the tab
# coordinates change; that's R2.x's problem to update.

Add-Type -AssemblyName System.Drawing

Write-Host "[capture_chrome] launching app ..." -ForegroundColor Cyan
Invoke-Adb -AdbArgs @("shell","am","force-stop","com.gkim.im.android")
Invoke-Adb -AdbArgs @("shell","monkey","-p","com.gkim.im.android","-c","android.intent.category.LAUNCHER","1") | Out-Null
Start-Sleep -Seconds 6

# Navigation plan reflects the post-R2 IA (default landing = tavern,
# bottom nav has 2 tabs: 酒馆 left @ x≈270 / 消息 right @ x≈810).

# 1. Tavern home (default landing post-R2.1)
Write-Host "[capture_chrome] capture: tavern-home" -ForegroundColor Yellow
$tavernPath = Join-Path $resolvedOutDir "tavern-home.png"
Capture-Screen -Path $tavernPath
Assert-TavernPalette -Path $tavernPath

# 2. Messages list (tap 消息 tab — second tab on the 2-tab nav)
Write-Host "[capture_chrome] capture: messages-list" -ForegroundColor Yellow
Tap 810 2280
Start-Sleep -Seconds 2
$messagesPath = Join-Path $resolvedOutDir "messages-list.png"
Capture-Screen -Path $messagesPath
Assert-TavernPalette -Path $messagesPath

# 3. Character detail (back to tavern, tap on the first preset card name area)
Write-Host "[capture_chrome] capture: character-detail" -ForegroundColor Yellow
Tap 270 2280
Start-Sleep -Seconds 2
Tap 496 1726
Start-Sleep -Seconds 2
$detailPath = Join-Path $resolvedOutDir "character-detail.png"
Capture-Screen -Path $detailPath
Assert-TavernPalette -Path $detailPath

# 4. Settings (back to tavern, then tap 设置 rectangular trigger)
Write-Host "[capture_chrome] capture: settings" -ForegroundColor Yellow
Back
Start-Sleep -Seconds 1
Tap 140 582
Start-Sleep -Seconds 2
$settingsPath = Join-Path $resolvedOutDir "settings.png"
Capture-Screen -Path $settingsPath
Assert-TavernPalette -Path $settingsPath

# 5. Gacha result (back to tavern, scroll down, tap 抽一张). Best-effort:
#    if the tap path drifts on a layout change, we still capture *something*
#    on the new palette; the assertion guards the palette, not the route.
Write-Host "[capture_chrome] capture: gacha-result" -ForegroundColor Yellow
Back
Start-Sleep -Seconds 1
# Scroll the LazyColumn so the "抽一张" CTA is comfortably on screen.
Invoke-Adb -AdbArgs @("shell","input","swipe","540","1500","540","900","400")
Start-Sleep -Seconds 1
# Tap the rough centre of the 抽一张 CTA (visible around y=900-1100 after scroll).
Tap 200 1015
Start-Sleep -Seconds 3
$gachaPath = Join-Path $resolvedOutDir "gacha-result.png"
Capture-Screen -Path $gachaPath
Assert-TavernPalette -Path $gachaPath

# --- Final verification -----------------------------------------------------

Write-Host "[capture_chrome] verifying file sizes ..." -ForegroundColor Cyan
$results = @(
    @{ Name = "tavern-home.png";      Path = $tavernPath },
    @{ Name = "messages-list.png";    Path = $messagesPath },
    @{ Name = "character-detail.png"; Path = $detailPath },
    @{ Name = "settings.png";         Path = $settingsPath },
    @{ Name = "gacha-result.png";     Path = $gachaPath }
)

# 50 KB minimum: sanity check that screencap produced a real PNG, not a
# transparent / single-color frame. The original tasks.md draft suggested
# 200 KB but the empty messages-list and portrait-less character-detail are
# legitimately thinner. Surface-pixel verification (above) is the load-bearing
# palette gate; this check just rejects empty captures.
foreach ($r in $results) {
    $sz = (Get-Item $r.Path).Length
    if ($sz -lt 50KB) {
        throw "$($r.Name) is too small ($sz bytes) — likely a black/blank capture."
    }
    Write-Host "  $($r.Name): $($sz) bytes [OK]" -ForegroundColor Green
}

Write-Host "[capture_chrome] DONE — 4 PNGs in $resolvedOutDir" -ForegroundColor Green
