[CmdletBinding()]
param(
    [string] $Profile = 'blog-platform',
    [string] $Namespace = 'blog-platform',
    [string] $PrometheusUrl = 'http://localhost:19090',
    [string] $ElasticsearchUrl = 'http://localhost:19200',
    [string] $TempoUrl = 'http://localhost:13200',
    [string] $BackupEvidencePath
)

$ErrorActionPreference = 'Stop'
$failures = [System.Collections.Generic.List[string]]::new()

function Add-CheckResult {
    param([string] $Name, [bool] $Passed, [string] $Detail)
    if ($Passed) {
        Write-Host "PASS  $Name - $Detail" -ForegroundColor Green
    } else {
        Write-Host "FAIL  $Name - $Detail" -ForegroundColor Red
        $failures.Add("$Name - $Detail")
    }
}

function Invoke-JsonGet {
    param([string] $Uri)
    try { return Invoke-RestMethod -Uri $Uri -TimeoutSec 10 }
    catch { $failures.Add("HTTP check failed for $Uri - $($_.Exception.Message)"); return $null }
}

$context = (& kubectl config current-context).Trim()
Add-CheckResult 'Kubernetes context' ($LASTEXITCODE -eq 0 -and $context -eq $Profile) "expected=$Profile actual=$context"
if ($context -ne $Profile) { throw "Refusing to inspect unexpected context '$context'." }

$requiredDeployments = @('identity-service', 'post-service', 'content-service', 'notification-service', 'api-gateway', 'frontend', 'blackbox-exporter', 'prometheus', 'grafana', 'alertmanager', 'tempo', 'otel-collector', 'kibana')
$deploymentJson = & kubectl -n $Namespace get deployments -o json | ConvertFrom-Json
foreach ($name in $requiredDeployments) {
    $item = $deploymentJson.items | Where-Object { $_.metadata.name -eq $name }
    $ready = $null -ne $item -and [int]$item.status.readyReplicas -ge [int]$item.spec.replicas
    Add-CheckResult "Deployment/$name" $ready "ready=$($item.status.readyReplicas) desired=$($item.spec.replicas)"
}

$requiredStatefulSets = @('identity-mysql', 'post-mysql', 'content-mysql', 'notification-mysql', 'kafka', 'elasticsearch')
$statefulJson = & kubectl -n $Namespace get statefulsets -o json | ConvertFrom-Json
foreach ($name in $requiredStatefulSets) {
    $item = $statefulJson.items | Where-Object { $_.metadata.name -eq $name }
    $ready = $null -ne $item -and [int]$item.status.readyReplicas -ge [int]$item.spec.replicas
    Add-CheckResult "StatefulSet/$name" $ready "ready=$($item.status.readyReplicas) desired=$($item.spec.replicas)"
}

$pvcJson = & kubectl -n $Namespace get pvc -o json | ConvertFrom-Json
$unbound = @($pvcJson.items | Where-Object { $_.status.phase -ne 'Bound' })
Add-CheckResult 'Persistent volumes' ($unbound.Count -eq 0 -and $pvcJson.items.Count -gt 0) "bound=$($pvcJson.items.Count - $unbound.Count) total=$($pvcJson.items.Count)"

$prometheusReady = Invoke-JsonGet "$PrometheusUrl/-/ready"
Add-CheckResult 'Prometheus readiness' ($null -ne $prometheusReady) $PrometheusUrl

$flags = Invoke-JsonGet "$PrometheusUrl/api/v1/status/flags"
$retention = if ($flags) { $flags.data.'storage.tsdb.retention.time' } else { $null }
Add-CheckResult 'Prometheus retention' ($retention -eq '35d') "configured=$retention expected=35d"

$probeQuery = [uri]::EscapeDataString('probe_success{job="synthetic-http"}')
$probes = Invoke-JsonGet "$PrometheusUrl/api/v1/query?query=$probeQuery"
$failedProbes = @($probes.data.result | Where-Object { [double]$_.value[1] -ne 1 })
Add-CheckResult 'Synthetic journeys' ($probes.data.result.Count -eq 3 -and $failedProbes.Count -eq 0) "healthy=$($probes.data.result.Count - $failedProbes.Count) expected=3"

$ruleQuery = [uri]::EscapeDataString('increase(prometheus_rule_evaluation_failures_total[5m])')
$ruleFailures = Invoke-JsonGet "$PrometheusUrl/api/v1/query?query=$ruleQuery"
$failedRules = @($ruleFailures.data.result | Where-Object { [double]$_.value[1] -gt 0 })
Add-CheckResult 'Prometheus rule evaluation' ($failedRules.Count -eq 0) "failure-series=$($failedRules.Count)"

$ilm = Invoke-JsonGet "$ElasticsearchUrl/_ilm/policy/blog-platform-logs-14-days"
Add-CheckResult 'Elasticsearch log retention' ($null -ne $ilm.'blog-platform-logs-14-days') 'policy=blog-platform-logs-14-days'

$tempoReady = Invoke-JsonGet "$TempoUrl/ready"
Add-CheckResult 'Tempo readiness' ($null -ne $tempoReady) $TempoUrl

if ($BackupEvidencePath) {
    $resolvedEvidence = Resolve-Path $BackupEvidencePath -ErrorAction SilentlyContinue
    Add-CheckResult 'Backup/restore evidence' ($null -ne $resolvedEvidence) "path=$BackupEvidencePath"
} else {
    Write-Host 'INFO  Backup/restore evidence - not required for this local run. Production gates must pass -BackupEvidencePath.' -ForegroundColor Cyan
}

if ($failures.Count -gt 0) {
    throw "Operational readiness failed with $($failures.Count) issue(s). Review the FAIL lines above."
}

Write-Host 'Operational readiness checks passed.' -ForegroundColor Green
