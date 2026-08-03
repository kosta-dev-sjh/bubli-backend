param(
    [Parameter(Mandatory = $true)]
    [string]$Baseline,
    [Parameter(Mandatory = $true)]
    [string]$Candidate,
    [string]$Output
)

$ErrorActionPreference = "Stop"

$baselineReport = Get-Content -LiteralPath (Resolve-Path -LiteralPath $Baseline) -Raw -Encoding UTF8 |
    ConvertFrom-Json
$candidateReport = Get-Content -LiteralPath (Resolve-Path -LiteralPath $Candidate) -Raw -Encoding UTF8 |
    ConvertFrom-Json

function New-Delta {
    param([double]$Before, [double]$After)
    return [ordered]@{
        baseline = [Math]::Round($Before, 6)
        candidate = [Math]::Round($After, 6)
        delta = [Math]::Round($After - $Before, 6)
    }
}

$comparison = [ordered]@{
    schemaVersion = 1
    baselineDataset = $baselineReport.datasetName
    candidateDataset = $candidateReport.datasetName
    sameDataset = $baselineReport.datasetName -eq $candidateReport.datasetName
    sameDatasetHash = $baselineReport.datasetSha256 -eq $candidateReport.datasetSha256
    sameTopK = $baselineReport.topK -eq $candidateReport.topK
    quality = [ordered]@{
        hitAtK = New-Delta $baselineReport.metrics.hitAtK $candidateReport.metrics.hitAtK
        recallAtK = New-Delta $baselineReport.metrics.recallAtK $candidateReport.metrics.recallAtK
        mrrAtK = New-Delta $baselineReport.metrics.mrrAtK $candidateReport.metrics.mrrAtK
        ndcgAtK = New-Delta $baselineReport.metrics.ndcgAtK $candidateReport.metrics.ndcgAtK
    }
    reliability = [ordered]@{
        errorRate = New-Delta $baselineReport.metrics.errorRate $candidateReport.metrics.errorRate
    }
    latencyMs = [ordered]@{
        average = New-Delta $baselineReport.metrics.latencyMs.average $candidateReport.metrics.latencyMs.average
        p50 = New-Delta $baselineReport.metrics.latencyMs.p50 $candidateReport.metrics.latencyMs.p50
        p95 = New-Delta $baselineReport.metrics.latencyMs.p95 $candidateReport.metrics.latencyMs.p95
        p99 = New-Delta $baselineReport.metrics.latencyMs.p99 $candidateReport.metrics.latencyMs.p99
    }
}

if (-not $comparison.sameDataset -or -not $comparison.sameDatasetHash -or -not $comparison.sameTopK) {
    Write-Warning "Dataset name and topK must match for a valid before/after comparison."
}

$json = $comparison | ConvertTo-Json -Depth 8
if ($Output) {
    $outputDirectory = Split-Path -Parent $Output
    if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory | Out-Null
    }
    $json | Set-Content -LiteralPath $Output -Encoding UTF8
}
$json
