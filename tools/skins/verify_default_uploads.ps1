<#
.SYNOPSIS
  HEAD-checks every default-skin URL against the CDN and asserts each
  returns 200 with the expected Content-Type + Cache-Control headers.

.DESCRIPTION
  R1.3 verification gate. Iterates the eight seeded characters and the
  four variant filenames; each combination must resolve to a 200 OK
  response with `Content-Type: image/png` and an immutable
  `Cache-Control` header. Prints a one-line OK / FAIL per URL and
  exits non-zero if any URL fails.

  -IncludeNonDefault extends the check to additional skins listed
  in `$AdditionalSkins` (R3.1 hook for the EPIC + LEGENDARY drops).
#>

[CmdletBinding()]
param(
    [switch] $IncludeNonDefault
)

$ErrorActionPreference = "Stop"

$CdnHost = "cdn.lastxuans.sbs"

$DefaultCharacters = @(
    "tavern-keeper",
    "architect-oracle",
    "sunlit-almoner",
    "midnight-sutler",
    "opal-lantern",
    "glass-mariner",
    "wandering-bard",
    "retired-veteran"
)

# R3.1: 8 EPIC/LEGENDARY skins live on R2 — one alternate per seeded
# character. Each pair below resolves to four CDN URLs
# (thumb / avatar / portrait / banner) that the -IncludeNonDefault
# pass HEAD-checks.
$AdditionalSkins = @(
    @{ CharacterId = "tavern-keeper";    SkinId = "crystal-host"      },  # LEGENDARY
    @{ CharacterId = "architect-oracle"; SkinId = "star-cartographer" },
    @{ CharacterId = "sunlit-almoner";   SkinId = "lavender-keeper"   },
    @{ CharacterId = "midnight-sutler";  SkinId = "ledger-courier"    },
    @{ CharacterId = "opal-lantern";     SkinId = "dawn-cartographer" },
    @{ CharacterId = "glass-mariner";    SkinId = "harbor-watcher"    },
    @{ CharacterId = "wandering-bard";   SkinId = "dawn-balladeer"    },
    @{ CharacterId = "retired-veteran";  SkinId = "hearth-watcher"    }
)

$Variants = @("thumb", "avatar", "portrait", "banner")

function Test-Url {
    param([string] $Url)
    try {
        $resp = Invoke-WebRequest -Uri $Url -Method Head -MaximumRedirection 0 -ErrorAction Stop
        $status = [int] $resp.StatusCode
        $contentType  = $resp.Headers["Content-Type"]
        if ($contentType -is [array]) { $contentType = $contentType[0] }
        $cacheControl = $resp.Headers["Cache-Control"]
        if ($cacheControl -is [array]) { $cacheControl = $cacheControl[0] }
        return [PSCustomObject]@{
            Url = $Url
            Ok = ($status -eq 200) -and ($contentType -eq "image/png") -and ($cacheControl -match "immutable")
            Status = $status
            ContentType = $contentType
            CacheControl = $cacheControl
        }
    }
    catch {
        return [PSCustomObject]@{
            Url = $Url
            Ok = $false
            Status = 0
            ContentType = ""
            CacheControl = ""
            Error = $_.Exception.Message
        }
    }
}

$urls = @()
foreach ($cid in $DefaultCharacters) {
    foreach ($v in $Variants) {
        $urls += "https://$CdnHost/character-skins/$cid/default/v1/$v.png"
    }
}
if ($IncludeNonDefault) {
    foreach ($pair in $AdditionalSkins) {
        foreach ($v in $Variants) {
            $urls += "https://$CdnHost/character-skins/$($pair.CharacterId)/$($pair.SkinId)/v1/$v.png"
        }
    }
}

Write-Host "[verify_default_uploads] checking $($urls.Count) URLs against $CdnHost ..." -ForegroundColor Cyan

$failed = 0
foreach ($url in $urls) {
    $r = Test-Url -Url $url
    if ($r.Ok) {
        Write-Host ("  [OK]   {0}" -f $r.Url) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0} (status={1}, ct='{2}', cc='{3}')" -f $r.Url, $r.Status, $r.ContentType, $r.CacheControl) -ForegroundColor Red
        $failed++
    }
}

if ($failed -gt 0) {
    Write-Host "[verify_default_uploads] FAILED — $failed of $($urls.Count) URLs did not pass" -ForegroundColor Red
    exit 1
}
Write-Host "[verify_default_uploads] PASS — all $($urls.Count) URLs return 200 image/png immutable" -ForegroundColor Green
