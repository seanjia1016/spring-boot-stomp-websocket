# 查看 Spring Boot 應用程式日誌
param(
    [int]$Lines = 100,
    [switch]$Follow,
    [switch]$All
)

$LogFile = "logs\spring-boot-stomp.log"

if (-not (Test-Path $LogFile)) {
    Write-Host "日誌文件不存在: $LogFile" -ForegroundColor Yellow
    Write-Host "請確保應用程式已啟動並配置了日誌輸出" -ForegroundColor Yellow
    exit
}

if ($All) {
    Write-Host "顯示所有日誌內容..." -ForegroundColor Green
    Get-Content -Path $LogFile
} elseif ($Follow) {
    Write-Host "實時監控日誌（按 Ctrl+C 停止）..." -ForegroundColor Green
    Get-Content -Path $LogFile -Wait -Tail $Lines
} else {
    Write-Host "顯示最後 $Lines 行日誌..." -ForegroundColor Green
    Get-Content -Path $LogFile -Tail $Lines
}








