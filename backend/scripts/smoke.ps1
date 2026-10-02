param(
    [string]$GatewayBase = "http://localhost:8080"
)

$loginBody = @{
    username = "admin"
    password = "admin123"
} | ConvertTo-Json

$loginResp = Invoke-RestMethod -Method Post -Uri "$GatewayBase/api/v1/auth/login" -ContentType "application/json" -Body $loginBody
$token = $loginResp.data.accessToken

if (-not $token) {
    throw "Login failed, no access token."
}

$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Method Get -Uri "$GatewayBase/api/v1/auth/me" -Headers $headers | Out-Null
Invoke-RestMethod -Method Get -Uri "$GatewayBase/api/v1/clubs" -Headers $headers | Out-Null

Write-Host "Smoke test passed."
