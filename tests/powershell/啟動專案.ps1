# Spring Boot STOMP WebSocket 專案啟動腳本
# 此腳本會啟動 Redis、RabbitMQ 和 Spring Boot 應用程式

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Spring Boot STOMP WebSocket 啟動腳本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Docker 是否運行
Write-Host "[1/4] 檢查 Docker 狀態..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker 正在運行" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker 未運行，請先啟動 Docker Desktop" -ForegroundColor Red
    Write-Host "  然後重新執行此腳本" -ForegroundColor Red
    exit 1
}

# 啟動 Redis
Write-Host ""
Write-Host "[2/4] 啟動 Redis..." -ForegroundColor Yellow
$redisContainer = docker ps -a --filter "name=redis" --format "{{.Names}}"
if ($redisContainer -eq "redis") {
    $redisRunning = docker ps --filter "name=redis" --format "{{.Names}}"
    if ($redisRunning -eq "redis") {
        Write-Host "✓ Redis 已在運行" -ForegroundColor Green
    } else {
        Write-Host "  啟動 Redis 容器..." -ForegroundColor Yellow
        docker start redis
        Start-Sleep -Seconds 2
        Write-Host "✓ Redis 已啟動" -ForegroundColor Green
    }
} else {
    Write-Host "  建立並啟動 Redis 容器..." -ForegroundColor Yellow
    docker run -d -p 6379:6379 --name redis redis:latest
    Start-Sleep -Seconds 3
    Write-Host "✓ Redis 已啟動" -ForegroundColor Green
}

# 啟動 RabbitMQ
Write-Host ""
Write-Host "[3/4] 啟動 RabbitMQ..." -ForegroundColor Yellow
$rabbitmqContainer = docker ps -a --filter "name=rabbitmq" --format "{{.Names}}"
if ($rabbitmqContainer -eq "rabbitmq") {
    $rabbitmqRunning = docker ps --filter "name=rabbitmq" --format "{{.Names}}"
    if ($rabbitmqRunning -eq "rabbitmq") {
        Write-Host "✓ RabbitMQ 已在運行" -ForegroundColor Green
    } else {
        Write-Host "  啟動 RabbitMQ 容器..." -ForegroundColor Yellow
        docker start rabbitmq
        Start-Sleep -Seconds 3
        Write-Host "✓ RabbitMQ 已啟動" -ForegroundColor Green
    }
} else {
    Write-Host "  建立並啟動 RabbitMQ 容器..." -ForegroundColor Yellow
    docker run -d -p 5672:5672 -p 15672:15672 --name rabbitmq rabbitmq:3-management
    Start-Sleep -Seconds 5
    Write-Host "✓ RabbitMQ 已啟動" -ForegroundColor Green
    Write-Host "  管理介面: http://localhost:15672 (guest/guest)" -ForegroundColor Cyan
}

# 等待服務就緒
Write-Host ""
Write-Host "[4/4] 等待服務就緒..." -ForegroundColor Yellow
Start-Sleep -Seconds 3

# 啟動 Spring Boot 應用程式
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  啟動 Spring Boot 應用程式..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "應用程式將在以下位置啟動：" -ForegroundColor Yellow
Write-Host "  - 主頁: http://localhost:8080" -ForegroundColor Green
Write-Host "  - WebSocket 端點: ws://localhost:8080/our-websocket" -ForegroundColor Green
Write-Host ""
Write-Host "按 Ctrl+C 停止應用程式" -ForegroundColor Yellow
Write-Host ""

# 切換到專案目錄
Set-Location $PSScriptRoot

# 啟動應用程式
mvn spring-boot:run



