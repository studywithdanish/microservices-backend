[CmdletBinding()]
param(
    [string] $Profile = 'blog-platform',
    [string] $Namespace = 'blog-platform',
    [string] $PrometheusUrl = 'http://localhost:19090',
    [ValidateRange(180, 600)] [int] $TimeoutSeconds = 300,
    [switch] $AcknowledgeImpact
)

$ErrorActionPreference = 'Stop'

if (-not $AcknowledgeImpact) {
    throw 'This drill temporarily scales the local frontend to zero. Re-run with -AcknowledgeImpact after reading docs/observability-phase-9.md.'
}

$context = (& kubectl config current-context).Trim()
if ($LASTEXITCODE -ne 0 -or $context -ne $Profile) {
    throw "Expected kubectl context '$Profile', but found '$context'."
}

try {
    Invoke-RestMethod -Uri "$PrometheusUrl/-/ready" -TimeoutSec 5 | Out-Null
} catch {
    throw "Prometheus is not reachable at $PrometheusUrl. Start 'kubectl -n $Namespace port-forward service/prometheus 19090:9090' in another terminal first."
}

$originalReplicas = [int]((& kubectl -n $Namespace get deployment frontend -o 'jsonpath={.spec.replicas}').Trim())
if ($LASTEXITCODE -ne 0 -or $originalReplicas -lt 1) {
    throw 'The frontend deployment must be healthy with at least one replica before the drill.'
}

$alertQuery = [uri]::EscapeDataString('ALERTS{alertname="BlogSyntheticProbeFailed",alertstate="firing",journey="frontend-home"}')
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$alertFired = $false

try {
    Write-Host 'Scaling frontend to zero to create a controlled synthetic failure...'
    & kubectl -n $Namespace scale deployment/frontend --replicas=0
    if ($LASTEXITCODE -ne 0) { throw 'Failed to scale the frontend deployment.' }

    do {
        Start-Sleep -Seconds 10
        $response = Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/query?query=$alertQuery" -TimeoutSec 10
        if ($response.status -eq 'success' -and $response.data.result.Count -gt 0) {
            $alertFired = $true
            Write-Host 'PASS: BlogSyntheticProbeFailed reached the firing state.' -ForegroundColor Green
            break
        }
        Write-Host 'Waiting for the two-minute alert hold period...'
    } while ((Get-Date) -lt $deadline)

    if (-not $alertFired) {
        throw "The expected alert did not fire within $TimeoutSeconds seconds."
    }
} finally {
    Write-Host "Restoring frontend to $originalReplicas replica(s)..."
    & kubectl -n $Namespace scale deployment/frontend "--replicas=$originalReplicas"
    if ($LASTEXITCODE -eq 0) {
        & kubectl -n $Namespace rollout status deployment/frontend --timeout=180s
    }
}

Write-Host 'Controlled synthetic-monitoring drill completed and the frontend was restored.' -ForegroundColor Green
