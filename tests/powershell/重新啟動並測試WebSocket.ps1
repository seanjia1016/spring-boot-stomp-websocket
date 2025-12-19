# 重新啟動應用程式並執行 WebSocket 測試

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  重新啟動應用程式並執行測試" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 切換到專案目錄
Set-Location $PSScriptRoot

# 1. 停止現有 Java 進程
Write-Host "[1/4] 停止現有 Java 進程..." -ForegroundColor Yellow
Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object { 
    $_.Path -like "*java*" 
} | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 2
Write-Host "  ✓ 已清理現有進程" -ForegroundColor Green

# 2. 編譯專案
Write-Host ""
Write-Host "[2/4] 編譯專案..." -ForegroundColor Yellow
mvn clean compile -DskipTests | Out-Null
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✓ 編譯成功" -ForegroundColor Green
} else {
    Write-Host "  ✗ 編譯失敗" -ForegroundColor Red
    exit 1
}

# 3. 啟動應用程式（在新窗口）
Write-Host ""
Write-Host "[3/4] 啟動應用程式..." -ForegroundColor Yellow
Write-Host "  應用程式將在新窗口啟動..." -ForegroundColor Cyan

# 創建啟動腳本
$startScript = @"
cd `"$PWD`"
mvn spring-boot:run
pause
"@
$startScriptPath = Join-Path $env:TEMP "start-app.ps1"
$startScript | Out-File -FilePath $startScriptPath -Encoding UTF8

# 在新窗口啟動應用程式
Start-Process powershell -ArgumentList "-NoExit", "-File", "`"$startScriptPath`"" -WindowStyle Normal

Write-Host "  ✓ 已在新窗口啟動應用程式" -ForegroundColor Green
Write-Host "  請等待應用程式啟動完成（約 30-60 秒）" -ForegroundColor Yellow

# 4. 等待應用程式啟動
Write-Host ""
Write-Host "[4/4] 等待應用程式啟動..." -ForegroundColor Yellow

$maxWait = 90
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
    Write-Host "  ⚠ 應用程式啟動超時" -ForegroundColor Yellow
    Write-Host "  請檢查新開啟的窗口是否有錯誤訊息" -ForegroundColor Yellow
    exit 1
}

# 5. 執行測試腳本
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  執行 WebSocket 測試" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Start-Sleep -Seconds 3
node WebSocket私信測試自動版.js

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  完成" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "應用程式正在新窗口運行" -ForegroundColor Yellow
Write-Host "關閉該窗口即可停止應用程式" -ForegroundColor Cyan
Write-Host ""



