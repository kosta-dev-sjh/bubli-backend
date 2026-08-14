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
    $ErrorActionPreference = "Continue"
    try {
        $value = & git -C $repositoryRoot @Arguments 2>$null
        if ($LASTEXITCODE -ne 0 -or $null -eq $value) {
            return $null
        }
        return [string]$value
    } catch {
        return $null
    }
}

function Get-Sha256Text {
    param([string]$Value)
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($(if ($null -eq $Value) { "" } else { $Value }))
        return (($sha256.ComputeHash($bytes) | ForEach-Object { $_.ToString("x2") }) -join "")
    } finally {
        $sha256.Dispose()
    }
}

function Get-GitWorkingTreeMetadata {
    $ErrorActionPreference = "Continue"
    try {
        $statusLines = @(& git -C $repositoryRoot -c core.quotePath=false status --porcelain=v1 --untracked-files=all 2>$null)
        if ($LASTEXITCODE -ne 0) {
            return [ordered]@{
                available = $false
                dirty = $null
                fingerprintSha256 = $null
                status = @()
                error = "git status failed"
            }
        }
        $trackedDiff = (& git -C $repositoryRoot diff --binary HEAD -- . 2>$null | Out-String)
        $untrackedFiles = @(& git -C $repositoryRoot -c core.quotePath=false ls-files --others --exclude-standard 2>$null)
        $untrackedHashes = @()
        foreach ($relativePath in $untrackedFiles) {
            $absolutePath = Join-Path $repositoryRoot $relativePath
            if (Test-Path -LiteralPath $absolutePath -PathType Leaf -ErrorAction SilentlyContinue) {
                $untrackedHashes += "{0}:{1}" -f $relativePath, (
                    Get-FileHash -LiteralPath $absolutePath -Algorithm SHA256 -ErrorAction Stop
                ).Hash
            }
        }
        $fingerprintInput = @(
            ($statusLines -join "`n"),
            $trackedDiff,
            ($untrackedHashes -join "`n")
        ) -join "`n---`n"
        return [ordered]@{
            available = $true
            dirty = $statusLines.Count -gt 0
            fingerprintSha256 = Get-Sha256Text $fingerprintInput
            status = $statusLines
            error = $null
        }
    } catch {
        return [ordered]@{
            available = $false
            dirty = $null
            fingerprintSha256 = $null
            status = @()
            error = $_.Exception.Message
        }
    }
}

function Get-RagEvaluationRequestBody {
    param($Case, [string]$CaseLocale)
    $request = @{
        roomId = $Case.roomId
        message = Get-CaseMessage $Case
        locale = $CaseLocale
        mode = if ($null -eq $Case.mode) { "ANSWER" } else { [string]$Case.mode }
        topK = $topK
    }
    if ($null -ne $Case.resourceIds) {
        $request.resourceIds = @($Case.resourceIds)
    }
    return $request | ConvertTo-Json -Depth 5
}

function Get-ExpectedOutcome {
    param($Case, [bool]$ExpectedGrounded)
    if ($null -ne $Case.expectedOutcome -and -not [string]::IsNullOrWhiteSpace([string]$Case.expectedOutcome)) {
        return ([string]$Case.expectedOutcome).ToUpperInvariant()
    }
    if ($ExpectedGrounded) {
        return "ANSWER"
    }
    return "NO_EVIDENCE"
}

function Get-ActualOutcome {
    param($Data, $Diagnostics)
    if ([bool]$Data.retrievalFailed) {
        return "RETRIEVAL_FAILED"
    }
    if ([bool]$Data.grounded) {
        return "ANSWER"
    }
    $status = $null
    if ($null -ne $Diagnostics -and $null -ne $Diagnostics.finalFusion) {
        $status = [string]$Diagnostics.finalFusion.answerabilityStatus
    }
    if ($status -eq "NEEDS_CLARIFICATION") {
        return "CLARIFY"
    }
    return "NO_EVIDENCE"
}

function Invoke-RagEvaluationCase {
    param($Case, [object[]]$RelevantItems, [bool]$ExpectedGrounded, [string]$CaseLocale)
    $body = Get-RagEvaluationRequestBody $Case $CaseLocale
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

function Receive-NextRagEvaluation {
    param([System.Collections.ArrayList]$Pending)
    $tasks = [System.Threading.Tasks.Task[]]@($Pending | ForEach-Object { $_.task })
    $completedTask = [System.Threading.Tasks.Task]::WhenAny($tasks).GetAwaiter().GetResult()
    $entry = $Pending | Where-Object {
        [object]::ReferenceEquals($_.task, $completedTask)
    } | Select-Object -First 1
    if ($null -eq $entry) {
        throw "Completed RAG evaluation request could not be matched to its case."
    }
    [void]$Pending.Remove($entry)
    $entry.stopwatch.Stop()
    $response = $null
    try {
        $response = $entry.task.GetAwaiter().GetResult()
        $responseText = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        if (-not $response.IsSuccessStatusCode) {
            throw "RAG evaluation API returned HTTP $([int]$response.StatusCode): $responseText"
        }
        $parsedResponse = $responseText | ConvertFrom-Json
        if (-not $parsedResponse.success) {
            throw "RAG evaluation API returned success=false"
        }
        $call = [pscustomobject]@{
            ok = $true
            data = $parsedResponse.data
            latencyMs = [Math]::Round($entry.stopwatch.Elapsed.TotalMilliseconds, 3)
            error = $null
        }
    } catch {
        $call = [pscustomobject]@{
            ok = $false
            data = $null
            latencyMs = [Math]::Round($entry.stopwatch.Elapsed.TotalMilliseconds, 3)
            error = $_.Exception.Message
        }
    } finally {
        if ($null -ne $response) {
            $response.Dispose()
        }
        $entry.request.Dispose()
    }
    return [pscustomobject]@{
        item = $entry.item
        call = $call
    }
}

function Invoke-RagEvaluationBatch {
    param([object[]]$Items)
    if ($Items.Count -eq 0) {
        return @()
    }
    if ($ConcurrencyLevel -le 1) {
        return @($Items | ForEach-Object {
            [pscustomobject]@{
                item = $_
                call = Invoke-RagEvaluationCase $_.case $_.relevantItems $_.expectedGrounded $_.locale
            }
        })
    }

    Add-Type -AssemblyName System.Net.Http
    $client = [System.Net.Http.HttpClient]::new()
    $client.Timeout = [TimeSpan]::FromSeconds($RequestTimeoutSec)
    $client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new(
        "Bearer",
        $BearerToken
    )
    $pending = [System.Collections.ArrayList]::new()
    $results = @()
    try {
        foreach ($item in $Items) {
            while ($pending.Count -ge $ConcurrencyLevel) {
                $results += Receive-NextRagEvaluation $pending
            }
            $request = [System.Net.Http.HttpRequestMessage]::new(
                [System.Net.Http.HttpMethod]::Post,
                [Uri]::new("$ApiBaseUrl/api/ai/evaluate-project-room-rag")
            )
            $request.Content = [System.Net.Http.StringContent]::new(
                (Get-RagEvaluationRequestBody $item.case $item.locale),
                [System.Text.Encoding]::UTF8,
                "application/json"
            )
            $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
            $task = $client.SendAsync($request)
            [void]$pending.Add([pscustomobject]@{
                item = $item
                request = $request
                task = $task
                stopwatch = $stopwatch
            })
        }
        while ($pending.Count -gt 0) {
            $results += Receive-NextRagEvaluation $pending
        }
    } finally {
        foreach ($entry in @($pending)) {
            $entry.request.Dispose()
        }
        $client.Dispose()
    }
    return @($results | Sort-Object { $_.item.ordinal })
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

$repositoryRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..\..")).Path
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

$evaluationItems = @()
$ordinal = 0
for ($runIndex = 1; $runIndex -le $RepeatCount; $runIndex++) {
    foreach ($case in $datasetDefinition.cases) {
        $relevantItems = @($case.relevant)
        $evaluationItems += [pscustomobject]@{
            ordinal = $ordinal
            runIndex = $runIndex
            case = $case
            relevantItems = $relevantItems
            expectedGrounded = Get-ExpectedGrounded $case $relevantItems
            locale = Get-CaseLocale $case
            intent = Get-CaseIntent $case
            expectedOutcome = Get-ExpectedOutcome $case (Get-ExpectedGrounded $case $relevantItems)
            expectedRetrievalModes = @($case.expectedRetrievalModes)
        }
        $ordinal++
    }
}

foreach ($evaluationResult in @(Invoke-RagEvaluationBatch $evaluationItems)) {
    $item = $evaluationResult.item
    $case = $item.case
    $runIndex = $item.runIndex
    $relevantItems = @($item.relevantItems)
    $expectedGrounded = [bool]$item.expectedGrounded
    $caseLocale = [string]$item.locale
    $caseIntent = [string]$item.intent
    $expectedOutcome = [string]$item.expectedOutcome
    $expectedRetrievalModes = @($item.expectedRetrievalModes)
    $call = $evaluationResult.call
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
        $actualIntent = if ($null -eq $diagnostics) { $null } else { [string]$diagnostics.queryIntent }
        $actualScopeConfidence = if ($null -eq $diagnostics) { $null } else { [string]$diagnostics.documentScopeConfidence }
        $actualOutcome = Get-ActualOutcome $data $diagnostics
        if (-not [string]::IsNullOrWhiteSpace([string]$ungroundedReason)) {
            Add-Count $languagePolicyRejectCounts ([string]$ungroundedReason)
        }

        $relevances = @($evidenceHits | ForEach-Object { Test-Relevant $_ $relevantItems })
        $relevantEvidenceCount = @($relevances | Where-Object { $_ }).Count
        $matchedRelevant = @{}
        $rankingMatchedRelevant = @{}
        $novelRelevances = @()
        $firstRelevantRank = $null
        for ($index = 0; $index -lt $evidenceHits.Count; $index++) {
            $novelRelevantAtRank = $false
            if (-not $relevances[$index]) {
                $novelRelevances += $false
                continue
            }
            if ($null -eq $firstRelevantRank) {
                $firstRelevantRank = $index + 1
            }
            Add-Count $modeHitContribution $evidenceHits[$index].retrievalMode
            foreach ($relevant in $relevantItems) {
                if (Test-Relevant $evidenceHits[$index] @($relevant)) {
                    $relevanceKey = Get-RelevanceKey $relevant
                    $matchedRelevant[$relevanceKey] = $true
                    if (-not $rankingMatchedRelevant.ContainsKey($relevanceKey)) {
                        $rankingMatchedRelevant[$relevanceKey] = $true
                        $novelRelevantAtRank = $true
                    }
                }
            }
            $novelRelevances += $novelRelevantAtRank
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
        $ndcg = if ($idcg -eq 0.0) {
            0.0
        } else {
            [Math]::Min(1.0, (Get-Dcg ([bool[]]$novelRelevances)) / $idcg)
        }
        $latencyMs = $call.latencyMs
        $latencies += $latencyMs
        $caseReports += [pscustomobject][ordered]@{
            id = $case.id
            runIndex = $runIndex
            locale = $caseLocale
            intent = $caseIntent
            actualIntent = $actualIntent
            intentCorrect = if ($caseIntent -eq "UNKNOWN") { $null } else { $actualIntent -eq $caseIntent }
            status = "success"
            latencyMs = $latencyMs
            expectedGrounded = $expectedGrounded
            actualGrounded = [bool]$data.grounded
            groundedCorrect = ([bool]$data.grounded) -eq $expectedGrounded
            expectedOutcome = $expectedOutcome
            actualOutcome = $actualOutcome
            outcomeCorrect = $actualOutcome -eq $expectedOutcome
            documentScopeConfidence = $actualScopeConfidence
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
            expectedOutcome = $expectedOutcome
            status = "error"
            latencyMs = $call.latencyMs
            expectedGrounded = $expectedGrounded
            error = $call.error
        }
    }
}

$successful = @($caseReports | Where-Object { $_.status -eq "success" })
$failed = @($caseReports | Where-Object { $_.status -eq "error" })
$qualityCases = @($successful | Where-Object { $null -ne $_.hitAtK })
$noAnswerCases = @($successful | Where-Object { $_.noAnswerCase })
$retrievalFailures = @($successful | Where-Object { $_.retrievalFailed })
$expectedModeCases = @($successful | Where-Object { $null -ne $_.expectedRetrievalModeHit })
$intentCases = @($successful | Where-Object { $null -ne $_.intentCorrect })
$groundingMetrics = Get-GroundingConfusionMetrics $successful
$applicationCommit = Get-GitValue @("rev-parse", "HEAD")
$applicationBranch = Get-GitValue @("rev-parse", "--abbrev-ref", "HEAD")
$workingTree = Get-GitWorkingTreeMetadata

$report = [ordered]@{
    schemaVersion = 4
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
        applicationGitMetadataAvailable = $workingTree.available
        applicationDirty = $workingTree.dirty
        applicationWorkingTreeSha256 = $workingTree.fingerprintSha256
        applicationWorkingTreeStatus = @($workingTree.status)
        applicationGitMetadataError = $workingTree.error
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
        outcomeAccuracy = Get-AverageMetric $successful "outcomeCorrect"
        intentRoutingAccuracy = if ($intentCases.Count -eq 0) { $null } else {
            Get-AverageMetric $intentCases "intentCorrect"
        }
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
