# Docker 服務啟動腳本
# 此腳本會啟動 Docker Desktop、建立並啟動 Redis 和 RabbitMQ 容器

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Docker 服務啟動腳本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Docker 是否安裝
Write-Host "[1/5] 檢查 Docker 安裝..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "✓ Docker 已安裝: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker 未安裝，請先安裝 Docker Desktop" -ForegroundColor Red
    Write-Host "  下載地址: https://www.docker.com/products/docker-desktop" -ForegroundColor Yellow
    exit 1
}

# 檢查 Docker Desktop 是否運行
Write-Host ""
Write-Host "[2/5] 檢查 Docker Desktop 狀態..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker Desktop 正在運行" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Desktop 未運行" -ForegroundColor Red
    Write-Host "  正在嘗試啟動 Docker Desktop..." -ForegroundColor Yellow
    
    # 嘗試啟動 Docker Desktop
    $dockerDesktopPath = "${env:ProgramFiles}\Docker\Docker\Docker Desktop.exe"
    if (Test-Path $dockerDesktopPath) {
        Start-Process $dockerDesktopPath
        Write-Host "  已啟動 Docker Desktop，請等待 30-60 秒後重新執行此腳本" -ForegroundColor Yellow
        Write-Host "  或手動啟動 Docker Desktop 後再執行此腳本" -ForegroundColor Yellow
        exit 0
    } else {
        Write-Host "  找不到 Docker Desktop，請手動啟動" -ForegroundColor Red
        exit 1
    }
}

# 切換到專案目錄
Set-Location $PSScriptRoot

# 檢查 docker-compose.yml 是否存在
Write-Host ""
Write-Host "[3/5] 檢查 docker-compose.yml..." -ForegroundColor Yellow
if (Test-Path "docker-compose.yml") {
    Write-Host "✓ docker-compose.yml 存在" -ForegroundColor Green
} else {
    Write-Host "✗ docker-compose.yml 不存在" -ForegroundColor Red
    exit 1
}

# 停止現有容器（如果存在）
Write-Host ""
Write-Host "[4/5] 停止現有容器..." -ForegroundColor Yellow
docker-compose down 2>&1 | Out-Null
Write-Host "✓ 已清理現有容器" -ForegroundColor Green

# 啟動服務
Write-Host ""
Write-Host "[5/5] 啟動 Docker 服務..." -ForegroundColor Yellow
Write-Host "  正在啟動 Redis 和 RabbitMQ..." -ForegroundColor Cyan
Write-Host ""

docker-compose up -d

# 等待服務啟動
Write-Host ""
Write-Host "等待服務啟動..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

# 檢查服務狀態
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  服務狀態檢查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Redis
$redisStatus = docker ps --filter "name=redis" --format "{{.Status}}"
if ($redisStatus) {
    Write-Host "✓ Redis: $redisStatus" -ForegroundColor Green
    Write-Host "  連接: localhost:6379" -ForegroundColor Cyan
} else {
    Write-Host "✗ Redis 啟動失敗" -ForegroundColor Red
}

# 檢查 RabbitMQ
$rabbitmqStatus = docker ps --filter "name=rabbitmq" --format "{{.Status}}"
if ($rabbitmqStatus) {
    Write-Host "✓ RabbitMQ: $rabbitmqStatus" -ForegroundColor Green
    Write-Host "  AMQP 連接: localhost:5672" -ForegroundColor Cyan
    Write-Host "  管理介面: http://localhost:15672" -ForegroundColor Cyan
    Write-Host "  帳號: guest / 密碼: guest" -ForegroundColor Cyan
} else {
    Write-Host "✗ RabbitMQ 啟動失敗" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  服務啟動完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "查看服務狀態: docker-compose ps" -ForegroundColor Yellow
Write-Host "查看日誌: docker-compose logs -f" -ForegroundColor Yellow
Write-Host "停止服務: docker-compose down" -ForegroundColor Yellow
Write-Host ""



