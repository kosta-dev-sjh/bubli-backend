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
    param($Before, $After)
    if ($null -eq $Before -or $null -eq $After) {
        return [ordered]@{
            baseline = $Before
            candidate = $After
            delta = $null
        }
    }
    return [ordered]@{
        baseline = [Math]::Round([double]$Before, 6)
        candidate = [Math]::Round([double]$After, 6)
        delta = [Math]::Round(([double]$After) - ([double]$Before), 6)
    }
}

function New-ModeDelta {
    param($BeforeMap, $AfterMap)
    $keys = @()
    if ($null -ne $BeforeMap) {
        $keys += $BeforeMap.PSObject.Properties.Name
    }
    if ($null -ne $AfterMap) {
        $keys += $AfterMap.PSObject.Properties.Name
    }
    $result = [ordered]@{}
    foreach ($key in @($keys | Sort-Object -Unique)) {
        $before = if ($null -eq $BeforeMap.$key) { 0.0 } else { [double]$BeforeMap.$key }
        $after = if ($null -eq $AfterMap.$key) { 0.0 } else { [double]$AfterMap.$key }
        $result[$key] = New-Delta $before $after
    }
    return $result
}

$comparison = [ordered]@{
    schemaVersion = 1
    baselineDataset = $baselineReport.datasetName
    candidateDataset = $candidateReport.datasetName
    sameDataset = $baselineReport.datasetName -eq $candidateReport.datasetName
    sameDatasetHash = $baselineReport.datasetSha256 -eq $candidateReport.datasetSha256
    sameTopK = $baselineReport.topK -eq $candidateReport.topK
    caseCounts = [ordered]@{
        caseCount = New-Delta $baselineReport.caseCount $candidateReport.caseCount
        qualityCaseCount = New-Delta $baselineReport.qualityCaseCount $candidateReport.qualityCaseCount
        noAnswerCaseCount = New-Delta $baselineReport.noAnswerCaseCount $candidateReport.noAnswerCaseCount
    }
    quality = [ordered]@{
        hitAtK = New-Delta $baselineReport.metrics.hitAtK $candidateReport.metrics.hitAtK
        recallAtK = New-Delta $baselineReport.metrics.recallAtK $candidateReport.metrics.recallAtK
        mrrAtK = New-Delta $baselineReport.metrics.mrrAtK $candidateReport.metrics.mrrAtK
        ndcgAtK = New-Delta $baselineReport.metrics.ndcgAtK $candidateReport.metrics.ndcgAtK
        contextPrecisionAtK = New-Delta `
            $baselineReport.metrics.contextPrecisionAtK `
            $candidateReport.metrics.contextPrecisionAtK
    }
    judgement = [ordered]@{
        groundedAccuracy = New-Delta $baselineReport.metrics.groundedAccuracy $candidateReport.metrics.groundedAccuracy
        noAnswerAccuracy = New-Delta $baselineReport.metrics.noAnswerAccuracy $candidateReport.metrics.noAnswerAccuracy
        precision = New-Delta `
            $baselineReport.metrics.grounding.precision `
            $candidateReport.metrics.grounding.precision
        recall = New-Delta `
            $baselineReport.metrics.grounding.recall `
            $candidateReport.metrics.grounding.recall
        f1 = New-Delta `
            $baselineReport.metrics.grounding.f1 `
            $candidateReport.metrics.grounding.f1
        noAnswerRecall = New-Delta `
            $baselineReport.metrics.grounding.noAnswerRecall `
            $candidateReport.metrics.grounding.noAnswerRecall
        balancedAccuracy = New-Delta `
            $baselineReport.metrics.grounding.balancedAccuracy `
            $candidateReport.metrics.grounding.balancedAccuracy
        expectedRetrievalModeAccuracy = New-Delta `
            $baselineReport.metrics.expectedRetrievalModeAccuracy `
            $candidateReport.metrics.expectedRetrievalModeAccuracy
    }
    reliability = [ordered]@{
        errorRate = New-Delta $baselineReport.metrics.errorRate $candidateReport.metrics.errorRate
        retrievalFailureRate = New-Delta $baselineReport.metrics.retrievalFailureRate $candidateReport.metrics.retrievalFailureRate
    }
    evidence = [ordered]@{
        documentEvidenceCountAverage = New-Delta `
            $baselineReport.metrics.documentEvidenceCountAverage `
            $candidateReport.metrics.documentEvidenceCountAverage
        retrievalModeSelectedCounts = New-ModeDelta `
            $baselineReport.metrics.retrievalModeSelectedCounts `
            $candidateReport.metrics.retrievalModeSelectedCounts
        retrievalModeHitContribution = New-ModeDelta `
            $baselineReport.metrics.retrievalModeHitContribution `
            $candidateReport.metrics.retrievalModeHitContribution
    }
    latencyMs = [ordered]@{
        average = New-Delta $baselineReport.metrics.latencyMs.average $candidateReport.metrics.latencyMs.average
        p50 = New-Delta $baselineReport.metrics.latencyMs.p50 $candidateReport.metrics.latencyMs.p50
        p95 = New-Delta $baselineReport.metrics.latencyMs.p95 $candidateReport.metrics.latencyMs.p95
        p99 = New-Delta $baselineReport.metrics.latencyMs.p99 $candidateReport.metrics.latencyMs.p99
    }
}

if (-not $comparison.sameDataset -or -not $comparison.sameDatasetHash -or -not $comparison.sameTopK) {
    Write-Warning "Dataset name, dataset hash, and topK must match for a valid before/after comparison."
}

$json = $comparison | ConvertTo-Json -Depth 12
if ($Output) {
    $outputDirectory = Split-Path -Parent $Output
    if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory)) {
        New-Item -ItemType Directory -Path $outputDirectory | Out-Null
    }
    $json | Set-Content -LiteralPath $Output -Encoding UTF8
}
$json
