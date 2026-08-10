param(
    [Parameter(Mandatory = $true)]
    [string]$Dataset,
    [Parameter(Mandatory = $true)]
    [string]$Output,
    [string]$ApiBaseUrl = "http://localhost:8080",
    [Parameter(Mandatory = $true)]
    [string]$BearerToken,
    [int]$WarmupCount = 0,
    [int]$RepeatCount = 1,
    [int]$ConcurrencyLevel = 1,
    [string]$RunLabel,
    [string]$DocumentSnapshot,
    [string]$ChunkingVersion,
    [string]$EmbeddingModelVersion,
    [string]$SearchConfig = "default",
    [int]$RequestTimeoutSec = 30,
    [switch]$IncludeEvidenceDetails
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
        if ($null -eq $relevant.chunkIndex -or $null -eq $Hit.chunkIndex) {
            return $true
        }
        if ([int]$relevant.chunkIndex -eq [int]$Hit.chunkIndex) {
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

function Get-AverageMetric {
    param([object[]]$Cases, [string]$Name)
    $values = @($Cases | ForEach-Object {
        if ($_.PSObject.Properties.Name -contains $Name -and $null -ne $_.$Name) {
            $_.$Name
        }
    })
    if ($values.Count -eq 0) {
        return 0.0
    }
    return [Math]::Round((($values | Measure-Object -Average).Average), 6)
}

function Get-Rate {
    param([double]$Numerator, [double]$Denominator)
    if ($Denominator -eq 0.0) {
        return 0.0
    }
    return [Math]::Round($Numerator / $Denominator, 6)
}

function Get-F1 {
    param([double]$Precision, [double]$Recall)
    if (($Precision + $Recall) -eq 0.0) {
        return 0.0
    }
    return [Math]::Round((2.0 * $Precision * $Recall) / ($Precision + $Recall), 6)
}

function Get-CaseLocale {
    param($Case)
    if ($null -ne $Case.locale -and -not [string]::IsNullOrWhiteSpace([string]$Case.locale)) {
        return [string]$Case.locale
    }
    if ([string]$Case.id -match '(^|-)en($|-)') {
        return "en-US"
    }
    if ([string]$Case.id -match '(^|-)ja($|-)') {
        return "ja-JP"
    }
    return "ko-KR"
}

function Get-CaseMessage {
    param($Case)
    if ($null -ne $Case.message -and -not [string]::IsNullOrWhiteSpace([string]$Case.message)) {
        return [string]$Case.message
    }
    return [string]$Case.query
}

function Get-ExpectedGrounded {
    param($Case, [object[]]$RelevantItems)
    if ($null -ne $Case.expectedGrounded) {
        return [bool]$Case.expectedGrounded
    }
    return $RelevantItems.Count -gt 0
}

function Get-CaseIntent {
    param($Case)
    if ($null -ne $Case.intent -and -not [string]::IsNullOrWhiteSpace([string]$Case.intent)) {
        return [string]$Case.intent
    }
    return "UNKNOWN"
}

function Get-GitValue {
    param([string[]]$Arguments)
    try {
        $value = & git @Arguments 2>$null
        if ($LASTEXITCODE -ne 0 -or $null -eq $value) {
            return $null
        }
        return [string]$value
    } catch {
        return $null
    }
}

function Invoke-RagEvaluationCase {
    param($Case, [object[]]$RelevantItems, [bool]$ExpectedGrounded, [string]$CaseLocale)
    $body = @{
        roomId = $Case.roomId
        message = Get-CaseMessage $Case
        locale = $CaseLocale
        mode = if ($null -eq $Case.mode) { "ANSWER" } else { [string]$Case.mode }
    } | ConvertTo-Json
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-RestMethod `
            -Method Post `
            -Uri "$ApiBaseUrl/api/ai/evaluate-project-room-rag" `
            -Headers $headers `
            -ContentType "application/json; charset=utf-8" `
            -TimeoutSec $RequestTimeoutSec `
            -Body $body
        $stopwatch.Stop()
        if (-not $response.success) {
            throw "RAG evaluation API returned success=false"
        }
        return [pscustomobject]@{
            ok = $true
            data = $response.data
            latencyMs = [Math]::Round($stopwatch.Elapsed.TotalMilliseconds, 3)
            error = $null
        }
    } catch {
        $stopwatch.Stop()
        return [pscustomobject]@{
            ok = $false
            data = $null
            latencyMs = [Math]::Round($stopwatch.Elapsed.TotalMilliseconds, 3)
            error = $_.Exception.Message
        }
    }
}

function Get-DocumentEvidenceHits {
    param($EvidenceItems)
    $hits = @()
    foreach ($evidence in @($EvidenceItems)) {
        if ([string]$evidence.sourceType -ne "DOCUMENT") {
            continue
        }
        $metadata = $evidence.metadata
        $hits += [pscustomobject][ordered]@{
            resourceId = $evidence.id
            chunkIndex = if ($null -eq $metadata.chunkIndex) { $null } else { [int]$metadata.chunkIndex }
            retrievalMode = if ($null -eq $metadata.retrievalMode) { "UNKNOWN" } else { [string]$metadata.retrievalMode }
            fusionScore = $metadata.fusionScore
            matchReason = $metadata.matchReason
        }
    }
    return $hits
}

function Add-Count {
    param([hashtable]$Table, [string]$Key, [double]$Amount = 1.0)
    if ([string]::IsNullOrWhiteSpace($Key)) {
        $Key = "UNKNOWN"
    }
    if (-not $Table.ContainsKey($Key)) {
        $Table[$Key] = 0.0
    }
    $Table[$Key] = [Math]::Round($Table[$Key] + $Amount, 6)
}

function Test-ExpectedRetrievalModeHit {
    param([object[]]$ExpectedModes, [object[]]$ActualModes)
    if ($ExpectedModes.Count -eq 0) {
        return $null
    }
    foreach ($actualMode in $ActualModes) {
        foreach ($expectedMode in $ExpectedModes) {
            if ([string]$actualMode -like "*$expectedMode*") {
                return $true
            }
        }
    }
    return $false
}

function Get-GroundingConfusionMetrics {
    param([object[]]$Cases)
    $tp = @($Cases | Where-Object { $_.expectedGrounded -and $_.actualGrounded }).Count
    $fp = @($Cases | Where-Object { -not $_.expectedGrounded -and $_.actualGrounded }).Count
    $tn = @($Cases | Where-Object { -not $_.expectedGrounded -and -not $_.actualGrounded }).Count
    $fn = @($Cases | Where-Object { $_.expectedGrounded -and -not $_.actualGrounded }).Count
    $precision = Get-Rate $tp ($tp + $fp)
    $recall = Get-Rate $tp ($tp + $fn)
    $specificity = Get-Rate $tn ($tn + $fp)
    [ordered]@{
        truePositive = $tp
        falsePositive = $fp
        trueNegative = $tn
        falseNegative = $fn
        precision = $precision
        recall = $recall
        f1 = Get-F1 $precision $recall
        noAnswerRecall = $specificity
        balancedAccuracy = [Math]::Round(($recall + $specificity) / 2.0, 6)
    }
}

function Get-GroupBreakdown {
    param([object[]]$Cases, [string]$PropertyName)
    $breakdown = [ordered]@{}
    foreach ($group in @($Cases | Group-Object -Property $PropertyName)) {
        $groupCases = @($group.Group)
        $qualityGroupCases = @($groupCases | Where-Object { $null -ne $_.hitAtK })
        $noAnswerGroupCases = @($groupCases | Where-Object { $_.noAnswerCase })
        $breakdown[$group.Name] = [ordered]@{
            caseCount = $groupCases.Count
            hitAtK = Get-AverageMetric $qualityGroupCases "hitAtK"
            recallAtK = Get-AverageMetric $qualityGroupCases "recallAtK"
            contextPrecisionAtK = Get-AverageMetric $groupCases "contextPrecisionAtK"
            groundedAccuracy = Get-AverageMetric $groupCases "groundedCorrect"
            noAnswerAccuracy = Get-AverageMetric $noAnswerGroupCases "noAnswerCorrect"
            grounding = Get-GroundingConfusionMetrics $groupCases
        }
    }
    return $breakdown
}

$datasetPath = (Resolve-Path -LiteralPath $Dataset).Path
$datasetDefinition = Get-Content -LiteralPath $datasetPath -Raw -Encoding UTF8 | ConvertFrom-Json
$topK = if ($null -eq $datasetDefinition.topK) { 5 } else { [int]$datasetDefinition.topK }
$headers = @{ Authorization = "Bearer $BearerToken" }
$RepeatCount = [Math]::Max(1, $RepeatCount)
$WarmupCount = [Math]::Max(0, $WarmupCount)
$ConcurrencyLevel = [Math]::Max(1, $ConcurrencyLevel)
$RequestTimeoutSec = [Math]::Max(1, $RequestTimeoutSec)
$caseReports = @()
$latencies = @()
$modeSelectedCounts = @{}
$modeHitContribution = @{}
$answerabilityReasonCounts = @{}
$languagePolicyRejectCounts = @{}

foreach ($case in @($datasetDefinition.cases | Select-Object -First $WarmupCount)) {
    $relevantItems = @($case.relevant)
    $expectedGrounded = Get-ExpectedGrounded $case $relevantItems
    $caseLocale = Get-CaseLocale $case
    Invoke-RagEvaluationCase $case $relevantItems $expectedGrounded $caseLocale | Out-Null
}

for ($runIndex = 1; $runIndex -le $RepeatCount; $runIndex++) {
foreach ($case in $datasetDefinition.cases) {
    $relevantItems = @($case.relevant)
    $expectedGrounded = Get-ExpectedGrounded $case $relevantItems
    $caseLocale = Get-CaseLocale $case
    $caseIntent = Get-CaseIntent $case
    $expectedRetrievalModes = @($case.expectedRetrievalModes)
    $call = Invoke-RagEvaluationCase $case $relevantItems $expectedGrounded $caseLocale
    if ($call.ok) {
        $data = $call.data
        $evidenceHits = @(Get-DocumentEvidenceHits $data.evidenceItems | Select-Object -First $topK)
        foreach ($hit in $evidenceHits) {
            Add-Count $modeSelectedCounts $hit.retrievalMode
        }

        $diagnostics = $data.retrievalDiagnostics
        $answerabilityReason = $null
        if ($null -ne $diagnostics -and $null -ne $diagnostics.finalFusion) {
            $answerabilityReason = [string]$diagnostics.finalFusion.answerabilityReason
            Add-Count $answerabilityReasonCounts $answerabilityReason
        }
        $ungroundedReason = if ($null -eq $diagnostics) { $null } else { $diagnostics.ungroundedReason }
        if (-not [string]::IsNullOrWhiteSpace([string]$ungroundedReason)) {
            Add-Count $languagePolicyRejectCounts ([string]$ungroundedReason)
        }

        $relevances = @($evidenceHits | ForEach-Object { Test-Relevant $_ $relevantItems })
        $relevantEvidenceCount = @($relevances | Where-Object { $_ }).Count
        $matchedRelevant = @{}
        $firstRelevantRank = $null
        for ($index = 0; $index -lt $evidenceHits.Count; $index++) {
            if (-not $relevances[$index]) {
                continue
            }
            if ($null -eq $firstRelevantRank) {
                $firstRelevantRank = $index + 1
            }
            Add-Count $modeHitContribution $evidenceHits[$index].retrievalMode
            foreach ($relevant in $relevantItems) {
                if (Test-Relevant $evidenceHits[$index] @($relevant)) {
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
        $latencyMs = $call.latencyMs
        $latencies += $latencyMs
        $caseReports += [pscustomobject][ordered]@{
            id = $case.id
            runIndex = $runIndex
            locale = $caseLocale
            intent = $caseIntent
            status = "success"
            latencyMs = $latencyMs
            expectedGrounded = $expectedGrounded
            actualGrounded = [bool]$data.grounded
            groundedCorrect = ([bool]$data.grounded) -eq $expectedGrounded
            noAnswerCase = -not $expectedGrounded
            noAnswerCorrect = if ($expectedGrounded) { $null } else { -not [bool]$data.grounded }
            retrievalFailed = [bool]$data.retrievalFailed
            retrievalFailureReason = $data.retrievalFailureReason
            ungroundedReason = $ungroundedReason
            hitAtK = if ($relevantCount -eq 0) { $null } elseif ($null -eq $firstRelevantRank) { 0.0 } else { 1.0 }
            recallAtK = if ($relevantCount -eq 0) { $null } else { [Math]::Round($recall, 6) }
            reciprocalRank = if ($relevantCount -eq 0) { $null } else { [Math]::Round($mrr, 6) }
            ndcgAtK = if ($relevantCount -eq 0) { $null } else { [Math]::Round($ndcg, 6) }
            contextPrecisionAtK = if ($evidenceHits.Count -eq 0) { $null } else {
                [Math]::Round($relevantEvidenceCount / $evidenceHits.Count, 6)
            }
            documentEvidenceCount = $evidenceHits.Count
            evidenceCount = $data.evidenceCount
            retrievalModes = @($data.retrievalModes)
            expectedRetrievalModes = $expectedRetrievalModes
            expectedRetrievalModeHit = Test-ExpectedRetrievalModeHit $expectedRetrievalModes @($data.retrievalModes)
            firstRelevantRank = $firstRelevantRank
            answerabilityReason = $answerabilityReason
            retrievalDiagnostics = $diagnostics
            evidenceDetails = if ($IncludeEvidenceDetails) { @($data.evidenceItems) } else { $null }
        }
    } else {
        $caseReports += [pscustomobject][ordered]@{
            id = $case.id
            runIndex = $runIndex
            locale = $caseLocale
            intent = $caseIntent
            status = "error"
            latencyMs = $call.latencyMs
            expectedGrounded = $expectedGrounded
            error = $call.error
        }
    }
}
}

$successful = @($caseReports | Where-Object { $_.status -eq "success" })
$failed = @($caseReports | Where-Object { $_.status -eq "error" })
$qualityCases = @($successful | Where-Object { $null -ne $_.hitAtK })
$noAnswerCases = @($successful | Where-Object { $_.noAnswerCase })
$retrievalFailures = @($successful | Where-Object { $_.retrievalFailed })
$expectedModeCases = @($successful | Where-Object { $null -ne $_.expectedRetrievalModeHit })
$groundingMetrics = Get-GroundingConfusionMetrics $successful
$applicationCommit = Get-GitValue @("rev-parse", "HEAD")
$applicationBranch = Get-GitValue @("rev-parse", "--abbrev-ref", "HEAD")

$report = [ordered]@{
    schemaVersion = 3
    datasetName = $datasetDefinition.name
    datasetSha256 = (Get-FileHash -LiteralPath $datasetPath -Algorithm SHA256).Hash.ToLowerInvariant()
    generatedAt = [DateTimeOffset]::UtcNow.ToString("o")
    apiBaseUrl = $ApiBaseUrl
    topK = $topK
    runLabel = $RunLabel
    warmupCount = $WarmupCount
    repeatCount = $RepeatCount
    concurrencyLevel = $ConcurrencyLevel
    requestTimeoutSec = $RequestTimeoutSec
    includeEvidenceDetails = [bool]$IncludeEvidenceDetails
    evaluationScope = "GROUNDING_ONLY"
    runMetadata = [ordered]@{
        applicationCommit = $applicationCommit
        applicationBranch = $applicationBranch
        documentSnapshot = $DocumentSnapshot
        chunkingVersion = $ChunkingVersion
        embeddingModelVersion = $EmbeddingModelVersion
        searchConfig = $SearchConfig
        powershellVersion = $PSVersionTable.PSVersion.ToString()
        os = [System.Environment]::OSVersion.VersionString
    }
    caseCount = $caseReports.Count
    successfulCaseCount = $successful.Count
    failedCaseCount = $failed.Count
    qualityCaseCount = $qualityCases.Count
    noAnswerCaseCount = $noAnswerCases.Count
    metrics = [ordered]@{
        hitAtK = Get-AverageMetric $qualityCases "hitAtK"
        recallAtK = Get-AverageMetric $qualityCases "recallAtK"
        mrrAtK = Get-AverageMetric $qualityCases "reciprocalRank"
        ndcgAtK = Get-AverageMetric $qualityCases "ndcgAtK"
        contextPrecisionAtK = Get-AverageMetric $successful "contextPrecisionAtK"
        groundedAccuracy = Get-AverageMetric $successful "groundedCorrect"
        noAnswerAccuracy = Get-AverageMetric $noAnswerCases "noAnswerCorrect"
        grounding = $groundingMetrics
        expectedRetrievalModeAccuracy = Get-AverageMetric $expectedModeCases "expectedRetrievalModeHit"
        answerQuality = [ordered]@{
            evaluated = $false
            reason = "Evaluation endpoint returns grounding context only. Final answer correctness, faithfulness, citation precision/recall, and locale match require an answer-generating endpoint or offline answer payload."
        }
        errorRate = if ($caseReports.Count -eq 0) { 0.0 } else {
            [Math]::Round($failed.Count / $caseReports.Count, 6)
        }
        retrievalFailureRate = if ($successful.Count -eq 0) { 0.0 } else {
            [Math]::Round($retrievalFailures.Count / $successful.Count, 6)
        }
        documentEvidenceCountAverage = Get-AverageMetric $successful "documentEvidenceCount"
        retrievalModeSelectedCounts = $modeSelectedCounts
        retrievalModeHitContribution = $modeHitContribution
        answerabilityReasonCounts = $answerabilityReasonCounts
        languagePolicyRejectCounts = $languagePolicyRejectCounts
        latencyMs = [ordered]@{
            average = if ($latencies.Count -eq 0) { $null } else {
                [Math]::Round((($latencies | Measure-Object -Average).Average), 3)
            }
            p50 = Get-Percentile $latencies 0.50
            p95 = Get-Percentile $latencies 0.95
            p99 = Get-Percentile $latencies 0.99
        }
        byLocale = Get-GroupBreakdown $successful "locale"
        byIntent = Get-GroupBreakdown $successful "intent"
    }
    cases = $caseReports
}

$outputDirectory = Split-Path -Parent $Output
if ($outputDirectory -and -not (Test-Path -LiteralPath $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}
$report | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath $Output -Encoding UTF8
$report.metrics | Format-List
