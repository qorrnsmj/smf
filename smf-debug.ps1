<#
.SYNOPSIS
Controls an SMF game started with .\gradlew.bat runGame --args="--debug-control".
.EXAMPLE
.\smf-debug.ps1 screenshot -Label stall
.EXAMPLE
.\smf-debug.ps1 teleport -Values 4,2,8
.EXAMPLE
.\smf-debug.ps1 look -Values -90,-15
.EXAMPLE
.\smf-debug.ps1 step -Values 60
#>
[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet('status', 'screenshot', 'teleport', 'move', 'look', 'pause', 'resume', 'step')]
    [string]$Command = 'status',
    [double[]]$Values = @(),
    [ValidatePattern('^[A-Za-z0-9_-]{1,64}$')]
    [string]$Label = 'game',
    [long]$GameProcessId = 0
)
$ErrorActionPreference = 'Stop'
$sessionDirectory = Join-Path $PSScriptRoot '.smf-debug'
$sessions = @(if (Test-Path -LiteralPath $sessionDirectory) {
    Get-ChildItem -LiteralPath $sessionDirectory -Filter '*.json' | ForEach-Object {
        try {
            $candidate = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
            if ($candidate.workspace -eq $PSScriptRoot -and
                ($GameProcessId -eq 0 -or $candidate.pid -eq $GameProcessId) -and
                (Get-Process -Id $candidate.pid -ErrorAction SilentlyContinue)) {
                $candidate
            }
        } catch { Write-Verbose "Ignoring invalid session file: $($_.Exception.Message)" }
    }
})
if ($sessions.Count -eq 0) {
    throw 'No running debug game in this worktree. Start: .\gradlew.bat runGame --args="--debug-control"'
}
if ($sessions.Count -ne 1) {
    throw "Multiple debug games found. Specify -GameProcessId. Available IDs: $($sessions.pid -join ', ')"
}
$session = $sessions[0]
if ($session.port -lt 1 -or $session.port -gt 65535) { throw 'Invalid session port' }
if ($Command -eq 'screenshot') {
    if ($Values.Count -ne 0) { throw 'Screenshot takes -Label, not -Values' }
    $body = "screenshot $Label"
} else {
    $numbers = @($Values | ForEach-Object { $_.ToString('R', [Globalization.CultureInfo]::InvariantCulture) })
    $body = (@($Command) + $numbers) -join ' '
}
$response = Invoke-RestMethod -Uri "http://127.0.0.1:$($session.port)/command" -Method Post `
    -Headers @{ 'X-SMF-Token' = $session.token } -ContentType 'text/plain; charset=utf-8' `
    -Body $body -TimeoutSec 35
if (-not $response.ok) { throw $response.error }
$response | ConvertTo-Json -Depth 8
