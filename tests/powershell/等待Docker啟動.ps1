# 等待 Docker 啟動並檢查服務

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  等待 Docker 啟動並檢查服務" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "Docker Desktop 正在啟動中..." -ForegroundColor Yellow
Write-Host "這可能需要 60-120 秒，請耐心等待" -ForegroundColor Yellow
Write-Host ""

$maxWait = 120  # 最多等待 120 秒
$elapsed = 0
$interval = 5   # 每 5 秒檢查一次

while ($elapsed -lt $maxWait) {
    try {
        docker ps | Out-Null
        Write-Host "✓ Docker 引擎已就緒！" -ForegroundColor Green
        break
    } catch {
        Write-Host "  等待中... ($elapsed 秒)" -ForegroundColor Gray
        Start-Sleep -Seconds $interval
        $elapsed += $interval
    }
}

if ($elapsed -ge $maxWait) {
    Write-Host ""
    Write-Host "✗ Docker 引擎啟動超時" -ForegroundColor Red
    Write-Host ""
    Write-Host "請檢查：" -ForegroundColor Yellow
    Write-Host "  1. Docker Desktop 是否正常顯示" -ForegroundColor Cyan
    Write-Host "  2. 系統托盤的 Docker 圖標狀態" -ForegroundColor Cyan
    Write-Host "  3. 是否有錯誤訊息" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "如果 Docker Desktop 顯示錯誤，請：" -ForegroundColor Yellow
    Write-Host "  1. 重啟 Docker Desktop" -ForegroundColor Cyan
    Write-Host "  2. 檢查 WSL 2 是否已安裝並啟用" -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "正在啟動 Redis 和 RabbitMQ..." -ForegroundColor Yellow
Set-Location $PSScriptRoot

docker-compose up -d

Write-Host ""
Write-Host "等待服務啟動..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  服務狀態" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

docker-compose ps

Write-Host ""
Write-Host "檢查服務連接..." -ForegroundColor Yellow

# 檢查 Redis
$redisRunning = docker ps --filter "name=redis" --format "{{.Names}}"
if ($redisRunning -eq "redis") {
    Write-Host "✓ Redis 正在運行" -ForegroundColor Green
} else {
    Write-Host "✗ Redis 未運行" -ForegroundColor Red
    Write-Host "  查看日誌: docker logs redis" -ForegroundColor Yellow
}

# 檢查 RabbitMQ
$rabbitmqRunning = docker ps --filter "name=rabbitmq" --format "{{.Names}}"
if ($rabbitmqRunning -eq "rabbitmq") {
    Write-Host "✓ RabbitMQ 正在運行" -ForegroundColor Green
    
    Write-Host ""
    Write-Host "等待 RabbitMQ 管理介面啟動（可能需要 30-60 秒）..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    # 測試管理介面
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:15672" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        Write-Host "✓ RabbitMQ 管理介面可訪問" -ForegroundColor Green
        Write-Host "  網址: http://localhost:15672" -ForegroundColor Cyan
        Write-Host "  帳號: guest / 密碼: guest" -ForegroundColor Cyan
    } catch {
        Write-Host "⚠ RabbitMQ 管理介面可能還在啟動中" -ForegroundColor Yellow
        Write-Host "  請稍後再試，或查看日誌: docker logs rabbitmq" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ RabbitMQ 未運行" -ForegroundColor Red
    Write-Host "  查看日誌: docker logs rabbitmq" -ForegroundColor Yellow
}

Write-Host ""



