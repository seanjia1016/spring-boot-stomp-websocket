# 檢查私信相關日誌

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  檢查私信相關日誌" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 找到最新的日誌文件
$logFile = Get-ChildItem "logs\spring-boot-stomp.log*" | Sort-Object LastWriteTime -Descending | Select-Object -First 1

if (-not $logFile) {
    Write-Host "找不到日誌文件！" -ForegroundColor Red
    exit 1
}

Write-Host "最新日誌文件: $($logFile.Name)" -ForegroundColor Green
Write-Host "文件大小: $([math]::Round($logFile.Length / 1KB, 2)) KB" -ForegroundColor Cyan
Write-Host ""

# 1. 檢查登入用戶 ID
Write-Host "[1] 檢查登入用戶 ID..." -ForegroundColor Yellow
$loginLogs = Get-Content $logFile.FullName | Select-String -Pattern "登入用戶 ID" | Select-Object -Last 5
if ($loginLogs) {
    Write-Host "找到 $($loginLogs.Count) 個登入記錄：" -ForegroundColor Green
    $loginLogs | ForEach-Object { 
        $userId = $_ -replace '.*登入用戶 ID: (\w+).*', '$1'
        Write-Host "  - $userId" -ForegroundColor Cyan
    }
} else {
    Write-Host "  未找到登入記錄" -ForegroundColor Red
}
Write-Host ""

# 2. 檢查私信訂閱
Write-Host "[2] 檢查私信訂閱..." -ForegroundColor Yellow
$subscribeLogs = Get-Content $logFile.FullName | Select-String -Pattern "SUBSCRIBE.*privateMessage" | Select-Object -Last 5
if ($subscribeLogs) {
    Write-Host "找到 $($subscribeLogs.Count) 個訂閱記錄：" -ForegroundColor Green
    $subscribeLogs | ForEach-Object { 
        $session = $_ -replace '.*session=(\w+).*', '$1'
        $user = $_ -replace '.*user=(\w+).*', '$1'
        Write-Host "  - Session: $session, User: $user" -ForegroundColor Cyan
    }
} else {
    Write-Host "  未找到訂閱記錄" -ForegroundColor Red
}
Write-Host ""

# 3. 檢查私信發送調試日誌
Write-Host "[3] 檢查私信發送調試日誌..." -ForegroundColor Yellow
$debugLogs = Get-Content $logFile.FullName | Select-String -Pattern "=== 私信發送調試 ===" -Context 5
if ($debugLogs) {
    Write-Host "找到私信發送記錄：" -ForegroundColor Green
    $debugLogs | ForEach-Object { Write-Host "  $_" -ForegroundColor Cyan }
} else {
    Write-Host "  未找到私信發送調試日誌" -ForegroundColor Red
    Write-Host "  這可能意味著私信沒有被處理" -ForegroundColor Yellow
}
Write-Host ""

# 4. 檢查是否有 SEND 訊息
Write-Host "[4] 檢查是否有發送到 /ws/privateMessage 的訊息..." -ForegroundColor Yellow
$sendLogs = Get-Content $logFile.FullName | Select-String -Pattern "SEND.*privateMessage|/ws/privateMessage" -Context 2
if ($sendLogs) {
    Write-Host "找到發送記錄：" -ForegroundColor Green
    $sendLogs | ForEach-Object { Write-Host "  $_" -ForegroundColor Cyan }
} else {
    Write-Host "  未找到發送記錄" -ForegroundColor Red
    Write-Host "  這可能意味著訊息沒有被發送到伺服器" -ForegroundColor Yellow
}
Write-Host ""

# 5. 檢查最近的日誌（最後 50 行）
Write-Host "[5] 最近的日誌（最後 50 行）..." -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Gray
Get-Content $logFile.FullName -Tail 50 | ForEach-Object { Write-Host $_ -ForegroundColor White }
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  檢查完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan



