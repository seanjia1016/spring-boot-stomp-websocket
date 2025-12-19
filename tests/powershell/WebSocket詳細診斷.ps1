# WebSocket 詳細診斷腳本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  WebSocket 詳細診斷" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 檢查應用程式
Write-Host "[1] 檢查應用程式..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 5 -UseBasicParsing
    Write-Host "  ✓ 應用程式運行中 (狀態碼: $($response.StatusCode))" -ForegroundColor Green
} catch {
    Write-Host "  ✗ 應用程式未運行" -ForegroundColor Red
    exit 1
}

# 2. 檢查 Redis
Write-Host ""
Write-Host "[2] 檢查 Redis..." -ForegroundColor Yellow
try {
    $ping = docker exec redis redis-cli ping 2>&1
    if ($ping -match "PONG") {
        Write-Host "  ✓ Redis 連接正常" -ForegroundColor Green
        
        # 檢查 Redis 頻道
        Write-Host "  檢查 Redis 頻道..." -ForegroundColor Cyan
        $channels = docker exec redis redis-cli PUBSUB CHANNELS 2>&1
        if ($channels) {
            Write-Host "  活動頻道: $channels" -ForegroundColor Green
        } else {
            Write-Host "  目前沒有活動頻道（這是正常的，如果沒有訊息發送）" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  ✗ Redis 連接失敗" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✗ 無法連接 Redis" -ForegroundColor Red
}

# 3. 測試 Redis Pub/Sub
Write-Host ""
Write-Host "[3] 測試 Redis Pub/Sub..." -ForegroundColor Yellow
Write-Host "  發送測試訊息到 Redis /topic/chat 頻道..." -ForegroundColor Cyan
$testMessage = '{"content":"測試訊息 ' + (Get-Date -Format 'HH:mm:ss') + '"}'
docker exec redis redis-cli PUBLISH "/topic/chat" $testMessage 2>&1 | Out-Null
Write-Host "  ✓ 測試訊息已發送到 Redis" -ForegroundColor Green
Write-Host "  請檢查專員A和專員B頁面是否收到此訊息" -ForegroundColor Cyan

# 4. 檢查 WebSocket 端點
Write-Host ""
Write-Host "[4] 檢查 WebSocket 端點..." -ForegroundColor Yellow
try {
    $wsUrl = "http://localhost:8080/our-websocket/info"
    $response = Invoke-WebRequest -Uri $wsUrl -TimeoutSec 5 -UseBasicParsing -ErrorAction SilentlyContinue
    if ($response.StatusCode -eq 200) {
        Write-Host "  ✓ WebSocket 端點可訪問" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠ WebSocket 端點可能無法通過 HTTP 訪問（這是正常的）" -ForegroundColor Yellow
}

# 5. 檢查前端頁面
Write-Host ""
Write-Host "[5] 前端頁面檢查..." -ForegroundColor Yellow
Write-Host "  請在瀏覽器中開啟以下頁面並檢查：" -ForegroundColor Cyan
Write-Host "  - 專員A: http://localhost:8080/agent-a.html" -ForegroundColor White
Write-Host "  - 專員B: http://localhost:8080/agent-b.html" -ForegroundColor White
Write-Host ""
Write-Host "  檢查項目：" -ForegroundColor Cyan
Write-Host "  1. 開啟瀏覽器開發者工具（F12）" -ForegroundColor White
Write-Host "  2. 切換到 Console 標籤" -ForegroundColor White
Write-Host "  3. 確認看到 '已連接' 狀態" -ForegroundColor White
Write-Host "  4. 確認看到用戶 ID" -ForegroundColor White
Write-Host "  5. 確認看到訂閱成功的日誌" -ForegroundColor White
Write-Host "  6. 切換到 Network 標籤" -ForegroundColor White
Write-Host "  7. 選擇 WebSocket 連接" -ForegroundColor White
Write-Host "  8. 查看 Messages 子標籤，確認有 CONNECT 和 SUBSCRIBE 訊息" -ForegroundColor White

# 6. 診斷建議
Write-Host ""
Write-Host "[6] 診斷建議..." -ForegroundColor Yellow
Write-Host ""
Write-Host "如果 WebSocket 無法連接，請檢查：" -ForegroundColor Cyan
Write-Host "  1. 應用程式日誌是否有錯誤" -ForegroundColor White
Write-Host "  2. 瀏覽器控制台是否有錯誤訊息" -ForegroundColor White
Write-Host "  3. Network 標籤中 WebSocket 連接的狀態" -ForegroundColor White
Write-Host "  4. 確認訂閱了正確的頻道：" -ForegroundColor White
Write-Host "     - 公共訊息: /topic/chat" -ForegroundColor Gray
Write-Host "     - 私信: /user/topic/privateMessage" -ForegroundColor Gray
Write-Host ""
Write-Host "如果訊息發送但收不到，請檢查：" -ForegroundColor Cyan
Write-Host "  1. Redis 是否正常運行" -ForegroundColor White
Write-Host "  2. RedisMessageListener 是否正確訂閱 /topic/chat" -ForegroundColor White
Write-Host "  3. 伺服器日誌是否有 '=== 公共訊息接收 ===' 或 '=== 私信發送調試 ==='" -ForegroundColor White
Write-Host "  4. 確認兩個頁面都訂閱了相同的頻道" -ForegroundColor White

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  診斷完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""



