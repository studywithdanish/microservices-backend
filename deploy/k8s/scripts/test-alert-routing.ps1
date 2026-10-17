[CmdletBinding()]
param(
    [string] $AlertmanagerUrl = 'http://localhost:9097',
    [string] $MailpitUrl = 'http://localhost:8025',
    [int] $TimeoutSeconds = 45
)

$ErrorActionPreference = 'Stop'
$alertmanagerBase = $AlertmanagerUrl.TrimEnd('/')
$mailpitBase = $MailpitUrl.TrimEnd('/')
$alertName = "Phase4RoutingSmokeTest-$([guid]::NewGuid().ToString('N').Substring(0, 8))"
$startedAt = [DateTime]::UtcNow

$initialInbox = Invoke-RestMethod -Uri "$mailpitBase/api/v1/messages" -TimeoutSec 5
$initialTotal = [int] $initialInbox.total

$alert = @{
    labels = @{
        alertname = $alertName
        severity = 'critical'
        team = 'platform'
        service = 'alertmanager'
        application = 'alertmanager'
    }
    annotations = @{
        summary = 'Phase 4 alert routing smoke test'
        description = 'Validates Alertmanager routing, notification templates, and SMTP delivery.'
        runbook = 'docs/observability-phase-4.md#end-to-end-validation'
    }
    startsAt = $startedAt.ToString('o')
    endsAt = $startedAt.AddMinutes(2).ToString('o')
}

$payload = ConvertTo-Json -InputObject @($alert) -Depth 6
Invoke-RestMethod `
    -Method Post `
    -Uri "$alertmanagerBase/api/v2/alerts" `
    -ContentType 'application/json' `
    -Body $payload | Out-Null

$deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
$deliveredMessage = $null
while ([DateTime]::UtcNow -lt $deadline) {
    $inbox = Invoke-RestMethod -Uri "$mailpitBase/api/v1/messages" -TimeoutSec 5
    $deliveredMessage = $inbox.messages |
        Where-Object { $_.Subject -eq "[FIRING][CRITICAL] $alertName" } |
        Select-Object -First 1
    if ($deliveredMessage) {
        break
    }
    Start-Sleep -Seconds 2
}

if (-not $deliveredMessage) {
    throw "No routed email for '$alertName' arrived within $TimeoutSeconds seconds."
}

$alert.endsAt = [DateTime]::UtcNow.ToString('o')
$resolvedPayload = ConvertTo-Json -InputObject @($alert) -Depth 6
Invoke-RestMethod `
    -Method Post `
    -Uri "$alertmanagerBase/api/v2/alerts" `
    -ContentType 'application/json' `
    -Body $resolvedPayload | Out-Null

Write-Host 'Alert routing smoke test passed.'
Write-Host "Alert: $alertName"
Write-Host "Receiver: on-call@blog-platform.local"
Write-Host "Subject: $($deliveredMessage.Subject)"
Write-Host "Inbox messages before/after: $initialTotal/$($inbox.total)"
