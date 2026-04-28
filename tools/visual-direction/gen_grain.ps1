# Generates res/raw/tavern_grain.png — a 1024 x 1024 LOW-FREQUENCY noise
# PNG used by Modifier.tavernGrain() as a paper / wood texture overlay.
# Compressible: we sample a coarse 32 x 32 random grid (fixed seed for
# reproducibility) and bilinearly interpolate up to 1024 x 1024 so the
# resulting PNG holds smooth gradients rather than per-pixel noise.

[CmdletBinding()]
param(
    [string] $OutPath = "$PSScriptRoot/../../android/app/src/main/res/drawable/tavern_grain.png"
)

Add-Type -AssemblyName System.Drawing

$gridSide = 33  # one extra column / row so we can interpolate to the right edge
$rand = New-Object System.Random 42
$grid = New-Object 'int[,]' $gridSide, $gridSide
for ($gy = 0; $gy -lt $gridSide; $gy++) {
    for ($gx = 0; $gx -lt $gridSide; $gx++) {
        $grid[$gy, $gx] = $rand.Next(96, 160)
    }
}

$bmp = New-Object System.Drawing.Bitmap 1024, 1024
$cellSize = 1024 / 32

for ($y = 0; $y -lt 1024; $y++) {
    $gy0 = [Math]::Floor($y / $cellSize)
    $gy1 = $gy0 + 1
    $fy = ($y / $cellSize) - $gy0
    for ($x = 0; $x -lt 1024; $x++) {
        $gx0 = [Math]::Floor($x / $cellSize)
        $gx1 = $gx0 + 1
        $fx = ($x / $cellSize) - $gx0
        $a = $grid[$gy0, $gx0]
        $b = $grid[$gy0, $gx1]
        $c = $grid[$gy1, $gx0]
        $d = $grid[$gy1, $gx1]
        $top = $a * (1 - $fx) + $b * $fx
        $bot = $c * (1 - $fx) + $d * $fx
        $v = [int]($top * (1 - $fy) + $bot * $fy)
        $color = [System.Drawing.Color]::FromArgb(80, $v, $v, $v)
        $bmp.SetPixel($x, $y, $color)
    }
}

$dir = [System.IO.Path]::GetDirectoryName([System.IO.Path]::GetFullPath($OutPath))
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir | Out-Null }
$bmp.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host "wrote $OutPath ($((Get-Item $OutPath).Length) bytes)"
