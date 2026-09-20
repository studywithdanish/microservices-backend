[CmdletBinding()]
param(
    [string] $Profile = 'blog-platform'
)

$ErrorActionPreference = 'Stop'
$backendRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..')).Path
$frontendRoot = (Resolve-Path (Join-Path $backendRoot '..\microservices-frontend')).Path

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

$images = [ordered]@{
    'blog-api-gateway:k8s' = (Join-Path $backendRoot 'gateway-service')
    'blog-identity-service:k8s' = (Join-Path $backendRoot 'identity-service')
    'blog-post-service:k8s' = (Join-Path $backendRoot 'post-service')
    'blog-content-service:k8s' = (Join-Path $backendRoot 'content-service')
    'blog-notification-service:k8s' = (Join-Path $backendRoot 'notification-service')
}

foreach ($entry in $images.GetEnumerator()) {
    Write-Host "Building $($entry.Key)"
    Invoke-Checked docker @('build', '--tag', $entry.Key, $entry.Value)
}

Write-Host 'Building blog-frontend:k8s with same-origin API routing'
Invoke-Checked docker @(
    'build',
    '--build-arg', 'REACT_APP_API_BASE_URL=/',
    '--tag', 'blog-frontend:k8s',
    $frontendRoot
)

$allImages = @($images.Keys) + 'blog-frontend:k8s'
foreach ($image in $allImages) {
    Write-Host "Loading $image into Minikube profile '$Profile'"
    Invoke-Checked minikube @('-p', $Profile, 'image', 'load', $image)
}

Write-Host "All application images are available inside Minikube profile '$Profile'."
