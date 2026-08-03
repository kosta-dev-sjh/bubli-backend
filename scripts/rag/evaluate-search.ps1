param(
    [Parameter(Mandatory = $true)]
    [string]$Dataset,
    [Parameter(Mandatory = $true)]
    [string]$Output,
    [string]$ApiBaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$BearerToken
)

$ErrorActionPreference = "Stop"

function Get-Percentile {
    param([double[]]$Values, [double]$Percentile)
    if ($Values.Count -eq 0) {
        return $null
    }
    $sorted = @($Values | Sort-Object)
    $index = [Math]::Max(0, [Math]::Ceiling($Percentile * $sorted.Count) - 1)
    return [Math]::Round($sorted[$index], 3)
}

function Get-RelevanceKey {
    param($Item)
    $chunk = if ($null -eq $Item.chunkIndex) { "*" } else { [string]$Item.chunkIndex }
    return "{0}:{1}" -f $Item.resourceId, $chunk
}

function Test-Relevant {
    param($Hit, $RelevantItems)
    foreach ($relevant in $RelevantItems) {
        if ([string]$relevant.resourceId -ne [string]$Hit.resourceId) {
            continue
        }
        if ($null -eq $relevant.chunkIndex -or [int]$relevant.chunkIndex -eq [int]$Hit.chunkIndex) {
            return $true
        }
    }
    return $false
}

function Get-Dcg {
    param([bool[]]$Relevances)
    $score = 0.0
    for ($index = 0; $index -lt $Relevances.Count; $index++) {
        if ($Relevances[$index]) {
            $score += 1.0 / [Math]::Log($index + 2, 2)
        }
    }
    return $score
}

$datasetPath = (Resolve-Path -LiteralPath $Dataset).Path
$datasetDefinition = Get-Content -LiteralPath $datasetPath -Raw -Encoding UTF8 | ConvertFrom-Json
$topK = if ($null -eq $datasetDefinition.topK) { 5 } else { [int]$datasetDefinition.topK }
$headers = @{ Authorization = "Bearer $BearerToken" }
$caseReports = @()
$latencies = @()

foreach ($case in $datasetDefinition.cases) {
    $body = @{
        scope = $case.scope
        roomId = $case.roomId
        query = $case.query
        topK = $topK
    } | ConvertTo-Json
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$ApiBaseUrl/api/ai/search-resource" `
            -Headers $headers `
            -ContentType "application/json; charset=utf-8" `
            -Body $body
        $stopwatch.Stop()
        if (-not $response.success) {
            throw "Search API returned success=false"
        }
        $hits = @($response.data.hits)
        $relevantItems = @($case.relevant)
        $relevances = @($hits | ForEach-Object { Test-Relevant $_ $relevantItems })
        $matchedRelevant = @{}
        $firstRelevantRank = $null
        for ($index = 0; $index -lt $hits.Count; $index++) {
            if (-not $relevances[$index]) {
                continue
            }
            if ($null -eq $firstRelevantRank) {
                $firstRelevantRank = $index + 1
            }
            foreach ($relevant in $relevantItems) {
                if (Test-Relevant $hits[$index] @($relevant)) {
                    $matchedRelevant[(Get-RelevanceKey $relevant)] = $true
                }
            }
        }
        $relevantCount = $relevantItems.Count
        $recall = if ($relevantCount -eq 0) { 0.0 } else { $matchedRelevant.Count / $relevantCount }
        $mrr = if ($null -eq $firstRelevantRank) { 0.0 } else { 1.0 / $firstRelevantRank }
        $idealCount = [Math]::Min($relevantCount, $topK)
        $ideal = @()
        for ($index = 0; $index -lt $topK; $index++) {
            $ideal += ($index -lt $idealCount)
        }
        $idcg = Get-Dcg ([bool[]]$ideal)
        $ndcg = if ($idcg -eq 0.0) { 0.0 } else { (Get-Dcg ([bool[]]$relevances)) / $idcg }
        $latencyMs = [Math]::Round($stopwatch.Elapsed.TotalMilliseconds, 3)
        $latencies += $latencyMs
        $caseReports += [pscustomobject][ordered]@{
            id = $case.id
            status = "success"
            latencyMs = $latencyMs
            hitAtK = if ($null -eq $firstRelevantRank) { 0.0 } else { 1.0 }
            recallAtK = [Math]::Round($recall, 6)
            reciprocalRank = [Math]::Round($mrr, 6)
            ndcgAtK = [Math]::Round($ndcg, 6)
            returnedHits = $hits.Count
            firstRelevantRank = $firstRelevantRank
        }
    } catch {
        $stopwatch.Stop()
        $caseReports += [pscustomobject][ordered]@{
            id = $case.id
            status = "error"
            latencyMs = [Math]::Round($stopwatch.Elapsed.TotalMilliseconds, 3)
            error = $_.Exception.Message
        }
    }
}

$successful = @($caseReports | Where-Object { $_.status -eq "success" })
$failed = @($caseReports | Where-Object { $_.status -eq "error" })
function Get-AverageMetric {
    param([object[]]$Cases, [string]$Name)
    if ($Cases.Count -eq 0) {
        return 0.0
    }
    return [Math]::Round((($Cases | Measure-Object -Property $Name -Average).Average), 6)
}

$report = [ordered]@{
    schemaVersion = 1
    datasetName = $datasetDefinition.name
    datasetSha256 = (Get-FileHash -LiteralPath $datasetPath -Algorithm SHA256).Hash.ToLowerInvariant()
    generatedAt = [DateTimeOffset]::UtcNow.ToString("o")
    apiBaseUrl = $ApiBaseUrl
    topK = $topK
    caseCount = $caseReports.Count
    successfulCaseCount = $successful.Count
    failedCaseCount = $failed.Count
    metrics = [ordered]@{
        hitAtK = Get-AverageMetric $successful "hitAtK"
        recallAtK = Get-AverageMetric $successful "recallAtK"
        mrrAtK = Get-AverageMetric $successful "reciprocalRank"
        ndcgAtK = Get-AverageMetric $successful "ndcgAtK"
        errorRate = if ($caseReports.Count -eq 0) { 0.0 } else {
            [Math]::Round($failed.Count / $caseReports.Count, 6)
        }
        latencyMs = [ordered]@{
            average = if ($latencies.Count -eq 0) { $null } else {
                [Math]::Round((($latencies | Measure-Object -Average).Average), 3)
            }
            p50 = Get-Percentile $latencies 0.50
            p95 = Get-Percentile $latencies 0.95
            p99 = Get-Percentile $latencies 0.99
        }
    }
    cases = $caseReports
}

$outputDirectory = Split-Path -Parent $Output
if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}
$report | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $Output -Encoding UTF8
$report.metrics | Format-List
