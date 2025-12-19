# WebSocket 測試腳本
# 此腳本用於測試 WebSocket 連接、訂閱和訊息發送

param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$WsUrl = "ws://localhost:8080/our-websocket"
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  WebSocket 測試腳本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查應用程式是否運行
Write-Host "[1/5] 檢查應用程式狀態..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri $BaseUrl -TimeoutSec 5 -UseBasicParsing -ErrorAction Stop
    Write-Host "✓ 應用程式正在運行 (狀態碼: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "✗ 應用程式未運行，請先啟動應用程式" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/5] 檢查 Redis 連接..." -ForegroundColor Yellow
try {
    $redisTest = docker exec redis redis-cli ping 2>&1
    if ($redisTest -match "PONG") {
        Write-Host "✓ Redis 連接正常" -ForegroundColor Green
    } else {
        Write-Host "⚠ Redis 可能未正常運行" -ForegroundColor Yellow
    }
} catch {
    Write-Host "⚠ 無法檢查 Redis 狀態" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[3/5] 檢查 RabbitMQ 連接..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:15672" -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ RabbitMQ 管理介面可訪問" -ForegroundColor Green
    }
} catch {
    Write-Host "⚠ RabbitMQ 管理介面可能無法訪問" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[4/5] 測試 REST API..." -ForegroundColor Yellow

# 測試發送公共訊息
Write-Host "  測試發送公共訊息..." -ForegroundColor Cyan
try {
    # WsController.sendMessage 期望的是 application/x-www-form-urlencoded 格式
    $body = @{ message = "測試訊息 $(Get-Date -Format 'HH:mm:ss')" }
    $response = Invoke-WebRequest -Uri "$BaseUrl/sendMessage" -Method POST -Body $body -UseBasicParsing -ErrorAction Stop
    Write-Host "  ✓ 公共訊息 API 調用成功 (狀態碼: $($response.StatusCode))" -ForegroundColor Green
    Write-Host "    訊息已發送，請檢查專員A和專員B頁面是否收到" -ForegroundColor Cyan
} catch {
    try {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "  ✗ 公共訊息 API 調用失敗 (狀態碼: $statusCode)" -ForegroundColor Red
    } catch {
        Write-Host "  ✗ 公共訊息 API 調用失敗" -ForegroundColor Red
    }
    Write-Host "    注意: REST API 錯誤不影響 WebSocket 功能，請直接在瀏覽器測試" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[5/5] 診斷資訊..." -ForegroundColor Yellow
Write-Host ""
Write-Host "請在瀏覽器中執行以下測試：" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 開啟專員A頁面: $BaseUrl/agent-a.html" -ForegroundColor Yellow
Write-Host "2. 開啟專員B頁面: $BaseUrl/agent-b.html" -ForegroundColor Yellow
Write-Host "3. 檢查瀏覽器控制台（F12）是否有錯誤" -ForegroundColor Yellow
Write-Host "4. 檢查 Network 標籤中的 WebSocket 訊息" -ForegroundColor Yellow
Write-Host ""
Write-Host "常見問題檢查：" -ForegroundColor Cyan
Write-Host "  - 確認兩個頁面都已連接（顯示'已連接'）" -ForegroundColor White
Write-Host "  - 確認兩個頁面都訂閱了正確的頻道" -ForegroundColor White
Write-Host "  - 檢查伺服器日誌是否有錯誤" -ForegroundColor White
Write-Host "  - 確認 Redis 正在運行" -ForegroundColor White
Write-Host ""

# 檢查應用程式日誌
Write-Host "檢查應用程式日誌（最後 20 行）..." -ForegroundColor Yellow
Write-Host "請查看應用程式控制台輸出" -ForegroundColor Cyan
Write-Host ""

