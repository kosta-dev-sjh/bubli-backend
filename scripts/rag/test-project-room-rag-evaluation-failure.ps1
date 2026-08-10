param(
    [Parameter(Mandatory = $true)]
    [string]$Dataset,
    [Parameter(Mandatory = $true)]
    [string]$Output,
    [string]$ApiBaseUrl = "http://localhost:1",
    [string]$BearerToken = "failure-injection-token"
)

$ErrorActionPreference = "Stop"

./scripts/rag/evaluate-project-room-rag.ps1 `
    -Dataset $Dataset `
    -Output $Output `
    -ApiBaseUrl $ApiBaseUrl `
    -BearerToken $BearerToken `
    -RunLabel "failure-injection" `
    -SearchConfig "failure-injection" `
    -RequestTimeoutSec 1

$report = Get-Content -LiteralPath $Output -Raw -Encoding UTF8 | ConvertFrom-Json
if ($report.failedCaseCount -eq 0) {
    throw "Expected at least one failed case for failure injection."
}
if ($report.metrics.errorRate -le 0) {
    throw "Expected errorRate to be greater than zero for failure injection."
}

$report.metrics | Format-List
