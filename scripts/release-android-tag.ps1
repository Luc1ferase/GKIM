[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$TagName,

    [string]$Remote = 'origin',

    [switch]$Watch
)

$modulePath = Join-Path $PSScriptRoot 'android-release-tools.psm1'
Import-Module $modulePath -Force

$result = Invoke-AndroidTagRelease -TagName $TagName -Remote $Remote -Watch:$Watch
$runContext = $result.RunContext

Write-Host "Android release tag pushed: $($result.TagInfo.TagName)"
Write-Host "Release asset name: $($result.TagInfo.AssetName)"
Write-Host "Repository: $($result.RepositorySlug)"
Write-Host "Remote branch: $($result.GitState.BranchName)"

if ($runContext.RunId) {
    Write-Host "GitHub Actions run: $($runContext.RunId)"
    Write-Host "Run URL: $($runContext.RunUrl)"
    Write-Host "Watch from terminal: $($runContext.WatchCommand)"
    Write-Host "Inspect in terminal: $($runContext.ViewCommand)"
}
else {
    Write-Host 'GitHub Actions run was not discovered automatically.'
    Write-Host "Open in browser: $($runContext.ActionsUrl)"
}

Write-Host "Check release asset in terminal: $($runContext.ReleaseCommand)"
Write-Host "Check release asset in browser: $($runContext.ReleaseUrl)"
