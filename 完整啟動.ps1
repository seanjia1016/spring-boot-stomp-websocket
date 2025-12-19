# 完整啟動腳本
# 此腳本會啟動 Docker 服務（Redis + RabbitMQ）並啟動 Spring Boot 應用程式

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Spring Boot STOMP 完整啟動腳本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 切換到專案目錄
Set-Location $PSScriptRoot

# 步驟 1: 啟動 Docker 服務
Write-Host "[步驟 1/3] 啟動 Docker 服務（Redis + RabbitMQ）..." -ForegroundColor Yellow
Write-Host ""

# 檢查 Docker 是否運行
try {
    docker ps | Out-Null
    Write-Host "✓ Docker Desktop 正在運行" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker Desktop 未運行" -ForegroundColor Red
    Write-Host "  請先啟動 Docker Desktop，然後重新執行此腳本" -ForegroundColor Yellow
    exit 1
}

# 啟動 Docker Compose 服務
Write-Host "  正在啟動 Redis 和 RabbitMQ..." -ForegroundColor Cyan
docker-compose up -d

# 等待服務啟動
Write-Host "  等待服務就緒..." -ForegroundColor Cyan
Start-Sleep -Seconds 5

# 檢查服務狀態
$redisStatus = docker ps --filter "name=redis" --format "{{.Status}}"
$rabbitmqStatus = docker ps --filter "name=rabbitmq" --format "{{.Status}}"

if ($redisStatus) {
    Write-Host "✓ Redis: $redisStatus" -ForegroundColor Green
} else {
    Write-Host "✗ Redis 啟動失敗" -ForegroundColor Red
}

if ($rabbitmqStatus) {
    Write-Host "✓ RabbitMQ: $rabbitmqStatus" -ForegroundColor Green
    Write-Host "  管理介面: http://localhost:15672 (guest/guest)" -ForegroundColor Cyan
} else {
    Write-Host "✗ RabbitMQ 啟動失敗" -ForegroundColor Red
}

Write-Host ""

# 步驟 2: 編譯專案
Write-Host "[步驟 2/3] 編譯 Spring Boot 專案..." -ForegroundColor Yellow
Write-Host ""

mvn clean compile -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ 編譯失敗" -ForegroundColor Red
    exit 1
}

Write-Host "✓ 編譯成功" -ForegroundColor Green
Write-Host ""

# 步驟 3: 啟動應用程式
Write-Host "[步驟 3/3] 啟動 Spring Boot 應用程式..." -ForegroundColor Yellow
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  應用程式資訊" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  主頁: http://localhost:8080" -ForegroundColor Green
Write-Host "  WebSocket: ws://localhost:8080/our-websocket" -ForegroundColor Green
Write-Host "  RabbitMQ 管理: http://localhost:15672" -ForegroundColor Green
Write-Host ""
Write-Host "按 Ctrl+C 停止應用程式" -ForegroundColor Yellow
Write-Host ""

# 啟動應用程式
mvn spring-boot:run



