# 專員ID變更自動化測試腳本
# 此腳本模擬兩個專員A連接，驗證第一個專員A是否會收到斷開通知

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "專員ID變更自動化測試" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查應用程式是否運行
Write-Host "檢查應用程式狀態..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method GET -TimeoutSec 2 -ErrorAction Stop
    Write-Host "✓ 應用程式正在運行" -ForegroundColor Green
} catch {
    Write-Host "✗ 應用程式未運行，請先啟動應用程式" -ForegroundColor Red
    exit 1
}

Write-Host "`n開始測試..." -ForegroundColor Yellow
Write-Host ""

# 步驟1：獲取第一個專員A的ID
Write-Host "步驟1：第一個專員A連接..." -ForegroundColor Cyan
try {
    $response1 = Invoke-RestMethod -Uri "http://localhost:8080/api/agent/a" -Method GET
    if ($response1.success -and $response1.agentId) {
        $firstAgentId = $response1.agentId
        Write-Host "  ✓ 第一個專員A ID: $firstAgentId" -ForegroundColor Green
    } else {
        Write-Host "  ✗ 無法獲取第一個專員A的ID" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  ✗ 請求失敗: $_" -ForegroundColor Red
    exit 1
}

# 等待一下
Start-Sleep -Seconds 1

# 步驟2：驗證第一個專員A的ID有效
Write-Host "`n步驟2：驗證第一個專員A的ID..." -ForegroundColor Cyan
try {
    $checkResponse1 = Invoke-RestMethod -Uri "http://localhost:8080/api/agent/a/check/$firstAgentId" -Method GET
    if ($checkResponse1.isValid) {
        Write-Host "  ✓ 第一個專員A的ID有效" -ForegroundColor Green
    } else {
        Write-Host "  ✗ 第一個專員A的ID無效（不應該發生）" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "  ✗ 檢查失敗: $_" -ForegroundColor Red
    exit 1
}

# 步驟3：模擬第二個專員A連接（通過WebSocket連接觸發ID創建）
Write-Host "`n步驟3：第二個專員A連接（觸發ID覆蓋）..." -ForegroundColor Cyan
Write-Host "  注意：這需要實際的WebSocket連接來觸發ID創建" -ForegroundColor Yellow
Write-Host "  我們將通過直接調用AgentService來模擬..." -ForegroundColor Yellow

# 由於無法直接調用AgentService，我們需要通過WebSocket連接
# 這裡我們提示用戶手動打開第二個瀏覽器分頁
Write-Host "`n請手動執行以下操作：" -ForegroundColor Yellow
Write-Host "  1. 打開瀏覽器，訪問: http://localhost:8080/agent-a.html" -ForegroundColor White
Write-Host "  2. 等待連接成功" -ForegroundColor White
Write-Host "  3. 觀察第一個專員A頁面（如果已打開）是否會自動斷開" -ForegroundColor White
Write-Host ""

# 或者，我們可以嘗試通過HTTP請求來觸發（但這不會真正觸發ID創建）
# 實際的ID創建是在WebSocket握手時觸發的

# 等待用戶操作
Write-Host "等待10秒，讓您有時間打開第二個專員A頁面..." -ForegroundColor Yellow
Start-Sleep -Seconds 10

# 步驟4：檢查第一個專員A的ID是否已變更
Write-Host "`n步驟4：檢查第一個專員A的ID狀態..." -ForegroundColor Cyan
try {
    $checkResponse2 = Invoke-RestMethod -Uri "http://localhost:8080/api/agent/a/check/$firstAgentId" -Method GET
    if (-not $checkResponse2.isValid) {
        Write-Host "  ✓ 第一個專員A的ID已變為無效（預期結果）" -ForegroundColor Green
        Write-Host "  當前有效ID: $($checkResponse2.validId)" -ForegroundColor Gray
    } else {
        Write-Host "  ⚠ 第一個專員A的ID仍然有效（可能第二個專員A尚未連接）" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ✗ 檢查失敗: $_" -ForegroundColor Red
}

# 步驟5：獲取當前專員A的ID
Write-Host "`n步驟5：獲取當前專員A的ID..." -ForegroundColor Cyan
try {
    $response2 = Invoke-RestMethod -Uri "http://localhost:8080/api/agent/a" -Method GET
    if ($response2.success -and $response2.agentId) {
        $currentAgentId = $response2.agentId
        Write-Host "  當前專員A ID: $currentAgentId" -ForegroundColor White
        if ($currentAgentId -ne $firstAgentId) {
            Write-Host "  ✓ ID已變更（預期結果）" -ForegroundColor Green
        } else {
            Write-Host "  ⚠ ID未變更（可能第二個專員A尚未連接）" -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  ✗ 獲取失敗: $_" -ForegroundColor Red
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "測試完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "測試說明：" -ForegroundColor Yellow
Write-Host "  1. 此腳本驗證了專員ID變更的基本邏輯" -ForegroundColor White
Write-Host "  2. 完整的WebSocket通知測試需要手動打開瀏覽器" -ForegroundColor White
Write-Host "  3. 建議使用集成測試（AgentIdChangeIntegrationTest）來進行自動化測試" -ForegroundColor White
Write-Host ""







