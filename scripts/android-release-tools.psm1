Set-StrictMode -Version Latest

function Get-AndroidReleaseTagInfo {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$TagName
    )

    if ($TagName -notmatch '^v(?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)$') {
        throw "Unsupported Android release tag '$TagName'. Expected format: vMAJOR.MINOR.PATCH."
    }

    [pscustomobject]@{
        TagName     = $TagName
        VersionName = "$($Matches.major).$($Matches.minor).$($Matches.patch)"
        AssetName   = "gkim-android-$TagName.apk"
    }
}

function Get-AndroidReleasePreflightResult {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$TagName,

        [Parameter(Mandatory = $true)]
        [AllowEmptyCollection()]
        [string[]]$GitStatusLines,

        [Parameter(Mandatory = $true)]
        [string]$BranchName,

        [string]$UpstreamName,

        [int]$AheadBy,

        [int]$BehindBy,

        [bool]$LocalTagExists,

        [bool]$RemoteTagExists
    )

    $issues = New-Object System.Collections.Generic.List[string]

    $dirtyLines = @($GitStatusLines | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($dirtyLines.Count -gt 0) {
        $issues.Add('Release tagging requires a clean worktree. Commit or stash local changes before continuing.')
    }

    if ([string]::IsNullOrWhiteSpace($UpstreamName)) {
        $issues.Add("Local branch '$BranchName' has no configured upstream. Configure a remote tracking branch before tagging a release.")
    }
    else {
        if ($AheadBy -gt 0) {
            $issues.Add("Local branch '$BranchName' is ahead of '$UpstreamName' by $AheadBy commit(s). Push the branch before tagging a release.")
        }

        if ($BehindBy -gt 0) {
            $issues.Add("Local branch '$BranchName' is behind '$UpstreamName' by $BehindBy commit(s). Pull or reconcile the branch before tagging a release.")
        }
    }

    if ($LocalTagExists -or $RemoteTagExists) {
        $issues.Add("Tag '$TagName' already exists locally or on the remote. Choose a new version before continuing.")
    }

    [pscustomobject]@{
        IsReady      = ($issues.Count -eq 0)
        BranchName   = $BranchName
        UpstreamName = $UpstreamName
        AheadBy      = $AheadBy
        BehindBy     = $BehindBy
        Issues       = @($issues)
    }
}

function Get-AndroidReleaseRunContext {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositorySlug,

        [Parameter(Mandatory = $true)]
        [string]$TagName,

        [Nullable[long]]$RunId,

        [string]$RunUrl
    )

    $releaseUrl = "https://github.com/$RepositorySlug/releases/tag/$TagName"
    $actionsQuery = [uri]::EscapeDataString("workflow:`"Android Tag Release`" event:push branch:$TagName")
    $actionsUrl = "https://github.com/$RepositorySlug/actions?query=$actionsQuery"

    $watchCommand = $null
    $viewCommand = $null
    if ($RunId) {
        $watchCommand = "gh run watch $RunId --interval 10 --exit-status"
        $viewCommand = "gh run view $RunId"
    }

    [pscustomobject]@{
        RepositorySlug = $RepositorySlug
        TagName        = $TagName
        RunId          = $RunId
        RunUrl         = $RunUrl
        ActionsUrl     = $actionsUrl
        WatchCommand   = $watchCommand
        ViewCommand    = $viewCommand
        ReleaseCommand = "gh release view $TagName --repo $RepositorySlug"
        ReleaseUrl     = $releaseUrl
    }
}

function Invoke-GitCommand {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [switch]$AllowFailure
    )

    $output = & git @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if (-not $AllowFailure -and $exitCode -ne 0) {
        $message = ($output | Out-String).Trim()
        throw "git $($Arguments -join ' ') failed with exit code $exitCode. $message"
    }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output   = @($output)
    }
}

function Invoke-GhCommand {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [switch]$AllowFailure
    )

    $output = & gh @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    if (-not $AllowFailure -and $exitCode -ne 0) {
        $message = ($output | Out-String).Trim()
        throw "gh $($Arguments -join ' ') failed with exit code $exitCode. $message"
    }

    [pscustomobject]@{
        ExitCode = $exitCode
        Output   = @($output)
    }
}

function Get-RepositorySlugFromRemoteUrl {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemoteUrl
    )

    $normalizedUrl = $RemoteUrl.Trim()
    $match = [regex]::Match($normalizedUrl, 'github\.com[:/](?<slug>[^/]+/[^/.]+?)(?:\.git)?$')
    if (-not $match.Success) {
        throw "Could not derive GitHub repository slug from remote URL '$RemoteUrl'."
    }

    $match.Groups['slug'].Value
}

function Get-AndroidReleaseGitState {
    [CmdletBinding()]
    param(
        [string]$Remote = 'origin',
        [string]$TagName
    )

    $branchName = ((Invoke-GitCommand -Arguments @('branch', '--show-current')).Output | Select-Object -First 1).Trim()
    if ([string]::IsNullOrWhiteSpace($branchName)) {
        throw 'Android release tagging requires a named branch. Detached HEAD is not supported.'
    }

    $statusLines = Invoke-GitCommand -Arguments @('status', '--short')
    $upstreamResult = Invoke-GitCommand -Arguments @('rev-parse', '--abbrev-ref', '--symbolic-full-name', '@{u}') -AllowFailure
    $upstreamName = $null
    $aheadBy = 0
    $behindBy = 0

    if ($upstreamResult.ExitCode -eq 0) {
        $upstreamName = ($upstreamResult.Output | Select-Object -First 1).Trim()
        $counts = ((Invoke-GitCommand -Arguments @('rev-list', '--left-right', '--count', "$upstreamName...HEAD")).Output | Select-Object -First 1).Trim()
        if ($counts) {
            $parts = $counts -split '\s+'
            if ($parts.Count -ge 2) {
                $behindBy = [int]$parts[0]
                $aheadBy = [int]$parts[1]
            }
        }
    }

    $localTagExists = (Invoke-GitCommand -Arguments @('show-ref', '--verify', '--quiet', "refs/tags/$TagName") -AllowFailure).ExitCode -eq 0
    $remoteTagOutput = Invoke-GitCommand -Arguments @('ls-remote', '--tags', $Remote, "refs/tags/$TagName") -AllowFailure
    $remoteTagExists = $remoteTagOutput.ExitCode -eq 0 -and (($remoteTagOutput.Output | Out-String).Trim().Length -gt 0)
    $remoteUrl = ((Invoke-GitCommand -Arguments @('remote', 'get-url', $Remote)).Output | Select-Object -First 1).Trim()

    [pscustomobject]@{
        BranchName      = $branchName
        UpstreamName    = $upstreamName
        AheadBy         = $aheadBy
        BehindBy        = $behindBy
        GitStatusLines  = @($statusLines.Output)
        LocalTagExists  = $localTagExists
        RemoteTagExists = $remoteTagExists
        RemoteUrl       = $remoteUrl
    }
}

function Wait-ForAndroidReleaseRun {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$RepositorySlug,

        [Parameter(Mandatory = $true)]
        [string]$TagName,

        [string]$WorkflowName = 'Android Tag Release',

        [int]$TimeoutSeconds = 90,

        [int]$PollIntervalSeconds = 5
    )

    $authStatus = Invoke-GhCommand -Arguments @('auth', 'status') -AllowFailure
    if ($authStatus.ExitCode -ne 0) {
        return $null
    }

    $deadline = [DateTimeOffset]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        $runsJson = Invoke-GhCommand -Arguments @('run', 'list', '--workflow', $WorkflowName, '--branch', $TagName, '--repo', $RepositorySlug, '--json', 'databaseId,url,status,conclusion,headBranch,headSha,displayTitle', '--limit', '10') -AllowFailure
        if ($runsJson.ExitCode -eq 0) {
            $raw = ($runsJson.Output | Out-String).Trim()
            if ($raw) {
                $runs = $raw | ConvertFrom-Json
                $matchingRun = $runs | Where-Object { $_.headBranch -eq $TagName } | Select-Object -First 1
                if ($matchingRun) {
                    return [pscustomobject]@{
                        RunId       = [long]$matchingRun.databaseId
                        RunUrl      = [string]$matchingRun.url
                        Status      = [string]$matchingRun.status
                        Conclusion  = [string]$matchingRun.conclusion
                        HeadBranch  = [string]$matchingRun.headBranch
                        HeadSha     = [string]$matchingRun.headSha
                        DisplayTitle = [string]$matchingRun.displayTitle
                    }
                }
            }
        }

        Start-Sleep -Seconds $PollIntervalSeconds
    } while ([DateTimeOffset]::UtcNow -lt $deadline)

    $null
}

function Invoke-AndroidTagRelease {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]
        [string]$TagName,

        [string]$Remote = 'origin',

        [string]$WorkflowName = 'Android Tag Release',

        [int]$RunLookupTimeoutSeconds = 90,

        [int]$RunLookupPollSeconds = 5,

        [switch]$Watch
    )

    $tagInfo = Get-AndroidReleaseTagInfo -TagName $TagName
    $gitState = Get-AndroidReleaseGitState -Remote $Remote -TagName $TagName
    $preflight = Get-AndroidReleasePreflightResult `
        -TagName $TagName `
        -GitStatusLines $gitState.GitStatusLines `
        -BranchName $gitState.BranchName `
        -UpstreamName $gitState.UpstreamName `
        -AheadBy $gitState.AheadBy `
        -BehindBy $gitState.BehindBy `
        -LocalTagExists $gitState.LocalTagExists `
        -RemoteTagExists $gitState.RemoteTagExists

    if (-not $preflight.IsReady) {
        throw ($preflight.Issues -join [Environment]::NewLine)
    }

    Invoke-GitCommand -Arguments @('push', $Remote, $gitState.BranchName) | Out-Null
    Invoke-GitCommand -Arguments @('tag', '-a', $TagName, '-m', "Android release $TagName") | Out-Null

    try {
        Invoke-GitCommand -Arguments @('push', $Remote, "refs/tags/$TagName") | Out-Null
    }
    catch {
        Invoke-GitCommand -Arguments @('tag', '-d', $TagName) -AllowFailure | Out-Null
        throw
    }

    $repositorySlug = Get-RepositorySlugFromRemoteUrl -RemoteUrl $gitState.RemoteUrl
    $run = Wait-ForAndroidReleaseRun `
        -RepositorySlug $repositorySlug `
        -TagName $TagName `
        -WorkflowName $WorkflowName `
        -TimeoutSeconds $RunLookupTimeoutSeconds `
        -PollIntervalSeconds $RunLookupPollSeconds

    $runContext = Get-AndroidReleaseRunContext `
        -RepositorySlug $repositorySlug `
        -TagName $TagName `
        -RunId $(if ($run) { $run.RunId } else { $null }) `
        -RunUrl $(if ($run) { $run.RunUrl } else { $null })

    if ($Watch -and $runContext.RunId) {
        Invoke-GhCommand -Arguments @('run', 'watch', [string]$runContext.RunId, '--interval', '10', '--exit-status') | Out-Null
    }

    [pscustomobject]@{
        TagInfo        = $tagInfo
        GitState       = $gitState
        Preflight      = $preflight
        RepositorySlug = $repositorySlug
        Run            = $run
        RunContext     = $runContext
    }
}

Export-ModuleMember -Function `
    Get-AndroidReleaseTagInfo, `
    Get-AndroidReleasePreflightResult, `
    Get-AndroidReleaseRunContext, `
    Invoke-AndroidTagRelease
