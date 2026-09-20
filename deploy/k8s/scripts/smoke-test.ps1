[CmdletBinding()]
param(
    [string] $BaseUrl,
    [string] $Profile = 'blog-platform'
)

$ErrorActionPreference = 'Stop'

if (-not $BaseUrl) {
    $minikubeIp = (& minikube -p $Profile ip).Trim()
    if ($LASTEXITCODE -ne 0 -or -not $minikubeIp) {
        throw 'Unable to determine the Minikube IP address.'
    }
    $BaseUrl = "http://${minikubeIp}:30080"
}

$BaseUrl = $BaseUrl.TrimEnd('/')
$deadline = (Get-Date).AddMinutes(3)
$health = $null
do {
    try {
        $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 10
        if ($health.status -eq 'UP') {
            break
        }
    } catch {
        Start-Sleep -Seconds 5
    }
} while ((Get-Date) -lt $deadline)

if ($health.status -ne 'UP') {
    throw "Gateway did not become healthy through $BaseUrl."
}

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$email = "k8s.smoke.$suffix@example.com"
$password = "K8s!$([guid]::NewGuid().ToString('N').Substring(0, 16))"
$jsonHeaders = @{ 'Content-Type' = 'application/json' }

$registrationBody = @{
        name = 'Kubernetes Smoke Test'
        email = $email
        password = $password
        about = 'Automated Minikube end-to-end verification'
} | ConvertTo-Json
$registrationRequest = @{
    Method = 'Post'
    Uri = "$BaseUrl/api/v1/auth/register"
    Headers = $jsonHeaders
    Body = $registrationBody
}
$registration = Invoke-RestMethod @registrationRequest

$loginRequest = @{
    Method = 'Post'
    Uri = "$BaseUrl/api/v1/auth/login"
    Headers = $jsonHeaders
    Body = (@{ username = $email; password = $password } | ConvertTo-Json)
}
$login = Invoke-RestMethod @loginRequest

$authorization = @{ Authorization = "Bearer $($login.token)" }
$currentUser = Invoke-RestMethod -Uri "$BaseUrl/api/v1/auth/me" -Headers $authorization
if ($currentUser.email -ne $email) {
    throw "Authenticated profile email '$($currentUser.email)' did not match '$email'."
}
$categories = @(Invoke-RestMethod -Uri "$BaseUrl/api/categories")
if (-not $categories.Count) {
    throw 'Content Service returned no categories.'
}

$postBody = @{
        title = "Kubernetes verification $suffix"
        content = 'Gateway, Identity, Post, and Content services are running on Minikube.'
        categoryId = $categories[0].categoryId
} | ConvertTo-Json
$postRequest = @{
    Method = 'Post'
    Uri = "$BaseUrl/api/posts"
    Headers = ($jsonHeaders + $authorization)
    Body = $postBody
}
$post = Invoke-RestMethod @postRequest

$notification = $null
$notificationDeadline = (Get-Date).AddSeconds(60)
do {
    $notifications = @(Invoke-RestMethod -Uri "$BaseUrl/api/notifications" -Headers $authorization)
    $notification = $notifications | Where-Object { $_.postId -eq $post.postId } | Select-Object -First 1
    if ($notification) {
        break
    }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $notificationDeadline)

if (-not $notification) {
    throw "Kafka notification for post '$($post.postId)' was not available within 60 seconds."
}

$commentRequest = @{
    Method = 'Post'
    Uri = "$BaseUrl/api/posts/$($post.postId)/comments"
    Headers = ($jsonHeaders + $authorization)
    Body = (@{ content = 'Kubernetes end-to-end smoke test passed.' } | ConvertTo-Json)
}
$comment = Invoke-RestMethod @commentRequest

[pscustomobject]@{
    BaseUrl = $BaseUrl
    Gateway = $health.status
    RegisteredUserId = $registration.id
    ProfileEmail = $currentUser.email
    CategoryCount = $categories.Count
    CreatedPostId = $post.postId
    KafkaNotificationId = $notification.id
    CreatedCommentId = $comment.id
} | Format-List
