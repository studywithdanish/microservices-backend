[CmdletBinding()]
param(
    [string] $ElasticsearchUrl = 'http://localhost:19200',
    [string] $KibanaUrl = 'http://localhost:15601',
    [string] $ServiceName = 'api-gateway',
    [int] $TimeoutSeconds = 90
)

$ErrorActionPreference = 'Stop'
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory)] [string] $Uri,
        [string] $Method = 'Get',
        [hashtable] $Headers = @{},
        [string] $Body
    )

    $arguments = @{
        Uri = $Uri
        Method = $Method
        Headers = $Headers
        UseBasicParsing = $true
    }
    if ($Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = $Body
    }

    Invoke-RestMethod @arguments
}

$cluster = Invoke-JsonRequest -Uri "$ElasticsearchUrl/_cluster/health?wait_for_status=yellow&timeout=10s"
if ($cluster.status -notin @('yellow', 'green')) {
    throw "Elasticsearch is not ready. Status: $($cluster.status)"
}

$dataView = Invoke-JsonRequest `
    -Uri "$KibanaUrl/api/data_views/data_view/blog-platform-logs" `
    -Headers @{ 'kbn-xsrf' = 'phase-5-smoke-test' }
if ($dataView.data_view.title -ne 'blog-platform-logs-*') {
    throw 'The Blog Platform Logs Kibana data view is not configured.'
}

$query = @{
    size = 1
    sort = @(@{ '@timestamp' = @{ order = 'desc' } })
    query = @{
        bool = @{
            filter = @(
                @{ term = @{ 'service.name' = $ServiceName } },
                @{ range = @{ '@timestamp' = @{ gte = 'now-15m' } } }
            )
        }
    }
} | ConvertTo-Json -Depth 8 -Compress

do {
    try {
        $result = Invoke-JsonRequest `
            -Uri "$ElasticsearchUrl/blog-platform-logs-*/_search" `
            -Method Post `
            -Body $query
        if ($result.hits.total.value -gt 0) {
            $source = $result.hits.hits[0]._source
            Write-Host "Centralized logging smoke test passed."
            Write-Host "Latest service: $($source.service.name)"
            Write-Host "Latest level: $($source.log.level)"
            Write-Host "Latest timestamp: $($source.'@timestamp')"
            exit 0
        }
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 404) {
            throw
        }
    }

    Start-Sleep -Seconds 5
} while ((Get-Date) -lt $deadline)

throw "No recent '$ServiceName' log was indexed before the timeout. Call the service once and rerun the test."
