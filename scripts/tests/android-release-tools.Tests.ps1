$modulePath = Join-Path $PSScriptRoot '..\android-release-tools.psm1'
Import-Module $modulePath -Force

Describe 'Get-AndroidReleaseTagInfo' {
    It 'accepts supported semantic version tags' {
        $info = Get-AndroidReleaseTagInfo -TagName 'v1.2.3'

        $info.TagName | Should Be 'v1.2.3'
        $info.VersionName | Should Be '1.2.3'
        $info.AssetName | Should Be 'gkim-android-v1.2.3.apk'
    }

    It 'rejects invalid release tags' {
        $threw = $false
        try {
            Get-AndroidReleaseTagInfo -TagName 'release-1.2.3' | Out-Null
        }
        catch {
            $threw = $true
        }

        $threw | Should Be $true
    }
}

Describe 'Get-AndroidReleasePreflightResult' {
    It 'blocks dirty worktrees, ahead branches, and conflicting tags' {
        $result = Get-AndroidReleasePreflightResult `
            -TagName 'v1.2.3' `
            -GitStatusLines @(' M android/README.md', '?? scripts/release-android-tag.ps1') `
            -BranchName 'master' `
            -UpstreamName 'origin/master' `
            -AheadBy 2 `
            -BehindBy 0 `
            -LocalTagExists $true `
            -RemoteTagExists $true

        $result.IsReady | Should Be $false
        $joinedIssues = $result.Issues -join "`n"
        $dirtyIssue = [regex]::Escape('Release tagging requires a clean worktree. Commit or stash local changes before continuing.')
        $aheadIssue = [regex]::Escape("Local branch 'master' is ahead of 'origin/master' by 2 commit(s). Push the branch before tagging a release.")
        $tagIssue = [regex]::Escape("Tag 'v1.2.3' already exists locally or on the remote. Choose a new version before continuing.")

        $joinedIssues | Should Match $dirtyIssue
        $joinedIssues | Should Match $aheadIssue
        $joinedIssues | Should Match $tagIssue
    }
}

Describe 'Get-AndroidReleaseRunContext' {
    It 'builds terminal and browser guidance for the pushed tag' {
        $context = Get-AndroidReleaseRunContext `
            -RepositorySlug 'Luc1ferase/GKIM' `
            -TagName 'v1.2.3' `
            -RunId 12345 `
            -RunUrl 'https://github.com/Luc1ferase/GKIM/actions/runs/12345'

        $context.WatchCommand | Should Be 'gh run watch 12345 --interval 10 --exit-status'
        $context.ViewCommand | Should Be 'gh run view 12345'
        $context.ReleaseCommand | Should Be 'gh release view v1.2.3 --repo Luc1ferase/GKIM'
        $context.ReleaseUrl | Should Be 'https://github.com/Luc1ferase/GKIM/releases/tag/v1.2.3'
    }
}
