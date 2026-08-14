param(
    [Parameter(Mandatory = $true)]
    [string]$Baseline,
    [Parameter(Mandatory = $true)]
    [string[]]$Candidates,
    [string]$Output
)

$ErrorActionPreference = "Stop"

function Read-Report {
    param([string]$Path)
    Get-Content -LiteralPath (Resolve-Path -LiteralPath $Path) -Raw -Encoding UTF8 | ConvertFrom-Json
}

function Get-Metric {
    param($Report, [string]$Path)
    $value = $Report
    foreach ($part in $Path.Split(".")) {
        if ($null -eq $value) {
            return $null
        }
        $value = $value.$part
    }
    return $value
}

function New-DeltaValue {
    param($Before, $After)
    if ($null -eq $Before -or $null -eq $After) {
        return $null
    }
    [Math]::Round(([double]$After) - ([double]$Before), 6)
}

$baselineReport = Read-Report $Baseline
$rows = @()

foreach ($candidatePath in $Candidates) {
    $candidateReport = Read-Report $candidatePath
    $sameDatasetHash = $baselineReport.datasetSha256 -eq $candidateReport.datasetSha256
    $metrics = [ordered]@{
        hitAtK = "metrics.hitAtK"
        recallAtK = "metrics.recallAtK"
        mrrAtK = "metrics.mrrAtK"
        ndcgAtK = "metrics.ndcgAtK"
        contextPrecisionAtK = "metrics.contextPrecisionAtK"
        groundedAccuracy = "metrics.groundedAccuracy"
        noAnswerAccuracy = "metrics.noAnswerAccuracy"
        precision = "metrics.grounding.precision"
        recall = "metrics.grounding.recall"
        f1 = "metrics.grounding.f1"
        noAnswerRecall = "metrics.grounding.noAnswerRecall"
        balancedAccuracy = "metrics.grounding.balancedAccuracy"
        retrievalFailureRate = "metrics.retrievalFailureRate"
        p95LatencyMs = "metrics.latencyMs.p95"
    }
    $row = [ordered]@{
        candidate = Split-Path $candidatePath -Leaf
        runLabel = $candidateReport.runLabel
        sameDatasetHash = $sameDatasetHash
        searchConfig = $candidateReport.runMetadata.searchConfig
        applicationCommit = $candidateReport.runMetadata.applicationCommit
    }
    foreach ($name in $metrics.Keys) {
        $path = $metrics[$name]
        $baselineValue = Get-Metric $baselineReport $path
        $candidateValue = Get-Metric $candidateReport $path
        $row[$name] = $candidateValue
        $row["${name}Delta"] = New-DeltaValue $baselineValue $candidateValue
    }
    $rows += [pscustomobject]$row
}

$report = [ordered]@{
    schemaVersion = 1
    baseline = Split-Path $Baseline -Leaf
    baselineDataset = $baselineReport.datasetName
    baselineDatasetSha256 = $baselineReport.datasetSha256
    generatedAt = [DateTimeOffset]::UtcNow.ToString("o")
    candidates = $rows
}

$json = $report | ConvertTo-Json -Depth 12
if ($Output) {
    $outputDirectory = Split-Path -Parent $Output
    if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory | Out-Null
    }
    $json | Set-Content -LiteralPath $Output -Encoding UTF8
}
$rows | Format-Table -AutoSize
$json
