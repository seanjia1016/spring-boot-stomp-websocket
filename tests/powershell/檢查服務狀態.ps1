# 檢查服務狀態腳本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  服務狀態檢查" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Docker
Write-Host "[1/3] 檢查 Docker..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker 正在運行" -ForegroundColor Green
} catch {
    Write-Host "✗ Docker 未運行" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 檢查 Redis
Write-Host "[2/3] 檢查 Redis..." -ForegroundColor Yellow
$redisStatus = docker ps --filter "name=redis" --format "{{.Status}}"
if ($redisStatus) {
    Write-Host "✓ Redis: $redisStatus" -ForegroundColor Green
    Write-Host "  連接: localhost:6379" -ForegroundColor Cyan
    
    # 測試連接
    $redisTest = docker exec redis redis-cli ping 2>&1
    if ($redisTest -match "PONG") {
        Write-Host "  ✓ Redis 回應正常" -ForegroundColor Green
    }
} else {
    Write-Host "✗ Redis 未運行" -ForegroundColor Red
    Write-Host "  啟動命令: docker-compose up -d redis" -ForegroundColor Yellow
}

Write-Host ""

# 檢查 RabbitMQ
Write-Host "[3/3] 檢查 RabbitMQ..." -ForegroundColor Yellow
$rabbitmqStatus = docker ps --filter "name=rabbitmq" --format "{{.Status}}"
if ($rabbitmqStatus) {
    Write-Host "✓ RabbitMQ: $rabbitmqStatus" -ForegroundColor Green
    Write-Host "  AMQP: localhost:5672" -ForegroundColor Cyan
    Write-Host "  管理介面: http://localhost:15672" -ForegroundColor Cyan
    Write-Host "  帳號: guest / 密碼: guest" -ForegroundColor Cyan
    
    # 測試管理介面
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:15672" -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
        Write-Host "  ✓ 管理介面可訪問 (狀態碼: $($response.StatusCode))" -ForegroundColor Green
    } catch {
        Write-Host "  ⚠ 管理介面可能還在啟動中" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ RabbitMQ 未運行" -ForegroundColor Red
    Write-Host "  啟動命令: docker-compose up -d rabbitmq" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""



