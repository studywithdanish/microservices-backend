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
        "--from-literal=NOTIFICATION_MYSQL_ROOT_PASSWORD=$([guid]::NewGuid().ToString('N'))"
    )
} else {
    Write-Host 'Reusing the existing blog-secrets Secret.'
    $notificationSecret = (& kubectl -n $namespace get secret blog-secrets -o 'jsonpath={.data.NOTIFICATION_DB_PASSWORD}').Trim()
    if (-not $notificationSecret) {
        $secretPatch = @{
            stringData = @{
                NOTIFICATION_DB_PASSWORD = [guid]::NewGuid().ToString('N')
                NOTIFICATION_MYSQL_ROOT_PASSWORD = [guid]::NewGuid().ToString('N')
            }
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

Invoke-Checked kubectl @(
    '-n', $namespace, 'apply',
    '-f', (Join-Path $k8sRoot 'applications.yaml'),
    '-f', (Join-Path $k8sRoot 'frontend.yaml')
)

foreach ($deployment in @('identity-service', 'post-service', 'content-service', 'notification-service', 'api-gateway', 'frontend')) {
    Invoke-Checked kubectl @('-n', $namespace, 'rollout', 'status', "deployment/$deployment", '--timeout=300s')
}

$minikubeIp = (& minikube -p $Profile ip).Trim()
Write-Host ''
Write-Host 'Kubernetes deployment completed successfully.'
Write-Host "Application URL: http://${minikubeIp}:30080"
Write-Host "Run the smoke test: .\deploy\k8s\scripts\smoke-test.ps1 -Profile $Profile"
