# 重新啟動 Spring Boot 應用程式並查看日誌
Write-Host "正在停止現有的應用程式..." -ForegroundColor Yellow

# 停止監聽 8080 端口的進程
$processes = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
foreach ($pid in $processes) {
    if ($pid) {
        Write-Host "停止進程 PID: $pid" -ForegroundColor Yellow
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
    }
}

Start-Sleep -Seconds 2

Write-Host "正在編譯應用程式..." -ForegroundColor Green
mvn clean compile -q

if ($LASTEXITCODE -ne 0) {
    Write-Host "編譯失敗！" -ForegroundColor Red
    exit 1
}

Write-Host "正在啟動應用程式並將日誌輸出到文件..." -ForegroundColor Green
Write-Host "日誌文件: logs\spring-boot-stomp.log" -ForegroundColor Cyan
Write-Host "按 Ctrl+C 停止應用程式" -ForegroundColor Yellow

# 確保日誌目錄存在
if (-not (Test-Path "logs")) {
    New-Item -ItemType Directory -Path "logs" | Out-Null
}

# 啟動應用程式並將輸出重定向到日誌文件
$logFile = "logs\spring-boot-stomp.log"
mvn spring-boot:run 2>&1 | Tee-Object -FilePath $logFile








