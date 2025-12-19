# 啟動應用程式並執行測試

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  啟動應用程式並執行測試" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查並停止現有進程
Write-Host "[1/4] 檢查並停止現有進程..." -ForegroundColor Yellow
$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue
if ($javaProcesses) {
    Write-Host "  發現 $($javaProcesses.Count) 個 Java 進程，正在停止..." -ForegroundColor Yellow
    $javaProcesses | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 2
    Write-Host "  ✓ 已停止現有進程" -ForegroundColor Green
} else {
    Write-Host "  ✓ 沒有運行中的 Java 進程" -ForegroundColor Green
}

# 檢查端口
Write-Host ""
Write-Host "[2/4] 檢查端口 8080..." -ForegroundColor Yellow
$port8080 = netstat -ano | findstr :8080
if ($port8080) {
    Write-Host "  ⚠ 端口 8080 被占用" -ForegroundColor Yellow
    Write-Host "  請手動停止占用端口的進程" -ForegroundColor Yellow
} else {
    Write-Host "  ✓ 端口 8080 可用" -ForegroundColor Green
}

# 編譯專案
Write-Host ""
Write-Host "[3/4] 編譯專案..." -ForegroundColor Yellow
mvn clean compile -DskipTests | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ 編譯成功" -ForegroundColor Green
} else {
    Write-Host "  ✗ 編譯失敗" -ForegroundColor Red
    exit 1
}

# 啟動應用程式
Write-Host ""
Write-Host "[4/4] 啟動應用程式..." -ForegroundColor Yellow
Write-Host "  應用程式將在背景啟動，請等待 30-45 秒..." -ForegroundColor Cyan
Write-Host ""

$job = Start-Job -ScriptBlock {
    Set-Location $using:PWD
    mvn spring-boot:run 2>&1
}

# 等待應用程式啟動
Write-Host "等待應用程式啟動..." -ForegroundColor Yellow
$maxWait = 60
$elapsed = 0
$started = $false

while ($elapsed -lt $maxWait -and -not $started) {
    Start-Sleep -Seconds 5
    $elapsed += 5
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080" -TimeoutSec 3 -UseBasicParsing -ErrorAction Stop
        Write-Host "  ✓ 應用程式已啟動！(狀態碼: $($response.StatusCode))" -ForegroundColor Green
        $started = $true
        break
    } catch {
        Write-Host "  等待中... ($elapsed 秒)" -ForegroundColor Gray
    }
}

if (-not $started) {
    Write-Host ""
    Write-Host "  ✗ 應用程式啟動超時" -ForegroundColor Red
    Write-Host "  請檢查應用程式日誌：" -ForegroundColor Yellow
    Write-Host "  Receive-Job -Job $($job.Id) | Select-Object -Last 50" -ForegroundColor Cyan
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  執行測試腳本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 執行測試腳本
& ".\WebSocket測試腳本.ps1"

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "應用程式正在背景運行（Job ID: $($job.Id)）" -ForegroundColor Yellow
Write-Host "查看日誌: Receive-Job -Job $($job.Id)" -ForegroundColor Cyan
Write-Host "停止應用程式: Stop-Job -Job $($job.Id); Remove-Job -Job $($job.Id)" -ForegroundColor Cyan
Write-Host ""



