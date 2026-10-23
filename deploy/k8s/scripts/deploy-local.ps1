[CmdletBinding()]
param(
    [switch] $SkipImageBuild,
    [string] $Profile = 'blog-platform'
)

$ErrorActionPreference = 'Stop'
$namespace = 'blog-platform'
$k8sRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

function Invoke-Checked {
    param(
        [Parameter(Mandatory)] [string] $FilePath,
        [Parameter(Mandatory)] [string[]] $ArgumentList
    )

    & $FilePath @ArgumentList
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath failed with exit code $LASTEXITCODE"
    }
}

$context = (& kubectl config current-context).Trim()
if ($LASTEXITCODE -ne 0 -or $context -ne $Profile) {
    throw "Expected kubectl context '$Profile', but found '$context'."
}

if (-not $SkipImageBuild) {
    & (Join-Path $PSScriptRoot 'build-images.ps1') -Profile $Profile
    if (-not $?) {
        throw 'Image build or Minikube image load failed.'
    }
}

Invoke-Checked kubectl @('apply', '-f', (Join-Path $k8sRoot 'namespace.yaml'))

& kubectl -n $namespace get secret blog-secrets *> $null
if ($LASTEXITCODE -ne 0) {
    $jwtSecret = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
    $internalToken = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))
    $identityPassword = [guid]::NewGuid().ToString('N')
    $postPassword = [guid]::NewGuid().ToString('N')
    $contentPassword = [guid]::NewGuid().ToString('N')
    $notificationPassword = [guid]::NewGuid().ToString('N')
    $grafanaPassword = [guid]::NewGuid().ToString('N')

    Invoke-Checked kubectl @(
        '-n', $namespace, 'create', 'secret', 'generic', 'blog-secrets',
        "--from-literal=JWT_SECRET=$jwtSecret",
        "--from-literal=INTERNAL_SERVICE_TOKEN=$internalToken",
        "--from-literal=IDENTITY_DB_PASSWORD=$identityPassword",
        "--from-literal=IDENTITY_MYSQL_ROOT_PASSWORD=$([guid]::NewGuid().ToString('N'))",
        "--from-literal=POST_DB_PASSWORD=$postPassword",
        "--from-literal=POST_MYSQL_ROOT_PASSWORD=$([guid]::NewGuid().ToString('N'))",
        "--from-literal=CONTENT_DB_PASSWORD=$contentPassword",
        "--from-literal=CONTENT_MYSQL_ROOT_PASSWORD=$([guid]::NewGuid().ToString('N'))",
        "--from-literal=NOTIFICATION_DB_PASSWORD=$notificationPassword",
        "--from-literal=NOTIFICATION_MYSQL_ROOT_PASSWORD=$([guid]::NewGuid().ToString('N'))",
        "--from-literal=GRAFANA_ADMIN_PASSWORD=$grafanaPassword"
    )
} else {
    Write-Host 'Reusing the existing blog-secrets Secret.'
    $secretUpdates = @{}
    $notificationSecret = (& kubectl -n $namespace get secret blog-secrets -o 'jsonpath={.data.NOTIFICATION_DB_PASSWORD}').Trim()
    if (-not $notificationSecret) {
        $secretUpdates.NOTIFICATION_DB_PASSWORD = [guid]::NewGuid().ToString('N')
        $secretUpdates.NOTIFICATION_MYSQL_ROOT_PASSWORD = [guid]::NewGuid().ToString('N')
    }

    $grafanaSecret = (& kubectl -n $namespace get secret blog-secrets -o 'jsonpath={.data.GRAFANA_ADMIN_PASSWORD}').Trim()
    if (-not $grafanaSecret) {
        $secretUpdates.GRAFANA_ADMIN_PASSWORD = [guid]::NewGuid().ToString('N')
    }

    if ($secretUpdates.Count -gt 0) {
        $secretPatch = @{
            stringData = $secretUpdates
        } | ConvertTo-Json -Compress
        Invoke-Checked kubectl @(
            '-n', $namespace, 'patch', 'secret', 'blog-secrets',
            '--type=merge', '-p', $secretPatch
        )
    }
}

Invoke-Checked kubectl @(
    '-n', $namespace, 'apply',
    '-f', (Join-Path $k8sRoot 'configmap.yaml'),
    '-f', (Join-Path $k8sRoot 'mysql.yaml'),
    '-f', (Join-Path $k8sRoot 'kafka.yaml')
)

foreach ($database in @('identity-mysql', 'post-mysql', 'content-mysql', 'notification-mysql', 'kafka')) {
    Invoke-Checked kubectl @('-n', $namespace, 'rollout', 'status', "statefulset/$database", '--timeout=300s')
}

Invoke-Checked kubectl @('-n', $namespace, 'delete', 'job', 'logging-setup', '--ignore-not-found=true', '--wait=true')
Invoke-Checked kubectl @('apply', '-k', $k8sRoot)

Invoke-Checked kubectl @('-n', $namespace, 'rollout', 'status', 'statefulset/elasticsearch', '--timeout=300s')

foreach ($deployment in @('identity-service', 'post-service', 'content-service', 'notification-service', 'api-gateway', 'frontend', 'mailpit', 'alertmanager', 'blackbox-exporter', 'prometheus', 'tempo', 'otel-collector', 'grafana', 'kibana')) {
    Invoke-Checked kubectl @('-n', $namespace, 'rollout', 'status', "deployment/$deployment", '--timeout=300s')
}

Invoke-Checked kubectl @('-n', $namespace, 'rollout', 'status', 'daemonset/filebeat', '--timeout=300s')
Invoke-Checked kubectl @('-n', $namespace, 'wait', '--for=condition=complete', 'job/logging-setup', '--timeout=300s')

$minikubeIp = (& minikube -p $Profile ip).Trim()
Write-Host ''
Write-Host 'Kubernetes deployment completed successfully.'
Write-Host "Application URL: http://${minikubeIp}:30080"
Write-Host "Run the smoke test: .\deploy\k8s\scripts\smoke-test.ps1 -Profile $Profile"
Write-Host 'Grafana access: kubectl -n blog-platform port-forward service/grafana 13001:3000'
Write-Host 'Grafana password (PowerShell): $encoded = kubectl -n blog-platform get secret blog-secrets -o jsonpath="{.data.GRAFANA_ADMIN_PASSWORD}"; [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encoded))'
Write-Host 'Alertmanager access: kubectl -n blog-platform port-forward service/alertmanager 19093:9093'
Write-Host 'Local alert inbox: kubectl -n blog-platform port-forward service/mailpit 18025:8025'
Write-Host 'Kibana access: kubectl -n blog-platform port-forward service/kibana 15601:5601'
Write-Host 'Elasticsearch diagnostics: kubectl -n blog-platform port-forward service/elasticsearch 19200:9200'
Write-Host 'Tempo diagnostics: kubectl -n blog-platform port-forward service/tempo 13200:3200'
Write-Host 'Readiness gate (after starting the operator port-forwards): .\deploy\k8s\scripts\verify-operational-readiness.ps1 -Profile blog-platform'
