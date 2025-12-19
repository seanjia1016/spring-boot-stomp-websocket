# 修復並啟動 Docker 服務腳本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Docker 服務修復與啟動" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Docker Desktop 進程
Write-Host "[步驟 1] 檢查 Docker Desktop 狀態..." -ForegroundColor Yellow
$dockerProcess = Get-Process "Docker Desktop" -ErrorAction SilentlyContinue

if ($dockerProcess) {
    Write-Host "✓ Docker Desktop 進程正在運行" -ForegroundColor Green
    Write-Host "  但 Docker 引擎可能尚未完全啟動" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "建議操作：" -ForegroundColor Yellow
    Write-Host "  1. 檢查系統托盤的 Docker 圖標" -ForegroundColor Cyan
    Write-Host "  2. 如果圖標顯示 'Docker Desktop is starting...'，請等待" -ForegroundColor Cyan
    Write-Host "  3. 如果圖標顯示錯誤，請重啟 Docker Desktop" -ForegroundColor Cyan
} else {
    Write-Host "✗ Docker Desktop 未運行" -ForegroundColor Red
    Write-Host ""
    Write-Host "正在啟動 Docker Desktop..." -ForegroundColor Yellow
    $dockerPath = "${env:ProgramFiles}\Docker\Docker\Docker Desktop.exe"
    if (Test-Path $dockerPath) {
        Start-Process $dockerPath
        Write-Host "✓ 已啟動 Docker Desktop" -ForegroundColor Green
        Write-Host "  請等待 60-90 秒讓 Docker 引擎完全啟動" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "[步驟 2] 等待 Docker 引擎啟動..." -ForegroundColor Yellow
Write-Host "  正在檢查 Docker 連接..." -ForegroundColor Cyan

$maxAttempts = 12
$attempt = 0
$dockerReady = $false

while ($attempt -lt $maxAttempts -and -not $dockerReady) {
    $attempt++
    Write-Host "  嘗試 $attempt/$maxAttempts..." -ForegroundColor Gray
    try {
        docker ps | Out-Null
        $dockerReady = $true
        Write-Host "✓ Docker 引擎已就緒！" -ForegroundColor Green
    } catch {
        Start-Sleep -Seconds 5
    }
}

if (-not $dockerReady) {
    Write-Host ""
    Write-Host "✗ Docker 引擎無法啟動" -ForegroundColor Red
    Write-Host ""
    Write-Host "請手動檢查：" -ForegroundColor Yellow
    Write-Host "  1. 打開 Docker Desktop 應用程式" -ForegroundColor Cyan
    Write-Host "  2. 檢查是否有錯誤訊息" -ForegroundColor Cyan
    Write-Host "  3. 嘗試重啟 Docker Desktop" -ForegroundColor Cyan
    Write-Host "  4. 檢查 Windows 功能中是否啟用 WSL 2" -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "[步驟 3] 檢查現有容器..." -ForegroundColor Yellow
$containers = docker ps -a --format "{{.Names}}\t{{.Status}}" 2>&1

if ($containers -match "redis|rabbitmq") {
    Write-Host "  發現現有容器：" -ForegroundColor Cyan
    docker ps -a --filter "name=redis" --format "  {{.Names}}\t{{.Status}}"
    docker ps -a --filter "name=rabbitmq" --format "  {{.Names}}\t{{.Status}}"
} else {
    Write-Host "  沒有發現 Redis 或 RabbitMQ 容器" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[步驟 4] 啟動 Docker Compose 服務..." -ForegroundColor Yellow

# 切換到專案目錄
Set-Location $PSScriptRoot

# 停止現有服務
Write-Host "  清理現有服務..." -ForegroundColor Cyan
docker-compose down 2>&1 | Out-Null

# 啟動服務
Write-Host "  啟動 Redis 和 RabbitMQ..." -ForegroundColor Cyan
docker-compose up -d

if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Docker Compose 服務已啟動" -ForegroundColor Green
} else {
    Write-Host "✗ Docker Compose 啟動失敗" -ForegroundColor Red
    exit 1
}

# 等待服務啟動
Write-Host ""
Write-Host "[步驟 5] 等待服務就緒..." -ForegroundColor Yellow
Start-Sleep -Seconds 8

# 檢查服務狀態
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  服務狀態檢查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Redis
Write-Host "Redis 狀態：" -ForegroundColor Yellow
$redisStatus = docker ps --filter "name=redis" --format "{{.Status}}"
if ($redisStatus) {
    Write-Host "  ✓ $redisStatus" -ForegroundColor Green
    Write-Host "  連接: localhost:6379" -ForegroundColor Cyan
    
    # 測試 Redis 連接
    $redisTest = docker exec redis redis-cli ping 2>&1
    if ($redisTest -match "PONG") {
        Write-Host "  ✓ Redis 回應正常" -ForegroundColor Green
    }
} else {
    Write-Host "  ✗ Redis 未運行" -ForegroundColor Red
    Write-Host "  查看日誌: docker logs redis" -ForegroundColor Yellow
}

Write-Host ""

# 檢查 RabbitMQ
Write-Host "RabbitMQ 狀態：" -ForegroundColor Yellow
$rabbitmqStatus = docker ps --filter "name=rabbitmq" --format "{{.Status}}"
if ($rabbitmqStatus) {
    Write-Host "  ✓ $rabbitmqStatus" -ForegroundColor Green
    Write-Host "  AMQP: localhost:5672" -ForegroundColor Cyan
    Write-Host "  管理介面: http://localhost:15672" -ForegroundColor Cyan
    Write-Host "  帳號: guest / 密碼: guest" -ForegroundColor Cyan
    
    # 等待 RabbitMQ 完全啟動
    Write-Host "  等待 RabbitMQ 管理介面就緒..." -ForegroundColor Gray
    Start-Sleep -Seconds 5
    
    # 測試 RabbitMQ 連接
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:15672" -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) {
            Write-Host "  ✓ RabbitMQ 管理介面可訪問" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⚠ RabbitMQ 管理介面可能還在啟動中，請稍後再試" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ RabbitMQ 未運行" -ForegroundColor Red
    Write-Host "  查看日誌: docker logs rabbitmq" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "如果 RabbitMQ 管理介面仍無法訪問：" -ForegroundColor Yellow
Write-Host "  1. 等待 30-60 秒後再試" -ForegroundColor Cyan
Write-Host "  2. 檢查日誌: docker logs rabbitmq" -ForegroundColor Cyan
Write-Host "  3. 重啟容器: docker restart rabbitmq" -ForegroundColor Cyan
Write-Host ""



