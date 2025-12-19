# 檢查並啟動 Docker Desktop 腳本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Docker Desktop 檢查與啟動" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Docker Desktop 是否安裝
Write-Host "[1/4] 檢查 Docker Desktop 安裝..." -ForegroundColor Yellow
$dockerDesktopPath = "${env:ProgramFiles}\Docker\Docker\Docker Desktop.exe"
$dockerDesktopPathX86 = "${env:ProgramFiles(x86)}\Docker\Docker\Docker Desktop.exe"

if (Test-Path $dockerDesktopPath) {
    Write-Host "✓ Docker Desktop 已安裝: $dockerDesktopPath" -ForegroundColor Green
    $dockerPath = $dockerDesktopPath
} elseif (Test-Path $dockerDesktopPathX86) {
    Write-Host "✓ Docker Desktop 已安裝: $dockerDesktopPathX86" -ForegroundColor Green
    $dockerPath = $dockerDesktopPathX86
} else {
    Write-Host "✗ Docker Desktop 未安裝" -ForegroundColor Red
    Write-Host ""
    Write-Host "請先安裝 Docker Desktop：" -ForegroundColor Yellow
    Write-Host "  下載地址: https://www.docker.com/products/docker-desktop" -ForegroundColor Cyan
    Write-Host "  或使用: winget install Docker.DockerDesktop" -ForegroundColor Cyan
    exit 1
}

# 檢查 Docker Desktop 是否運行
Write-Host ""
Write-Host "[2/4] 檢查 Docker Desktop 運行狀態..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "✓ Docker Desktop 正在運行" -ForegroundColor Green
    Write-Host ""
    Write-Host "檢查容器狀態..." -ForegroundColor Yellow
    docker ps -a
    exit 0
} catch {
    Write-Host "✗ Docker Desktop 未運行或未啟動完成" -ForegroundColor Red
}

# 檢查 Docker Desktop 進程
Write-Host ""
Write-Host "[3/4] 檢查 Docker Desktop 進程..." -ForegroundColor Yellow
$dockerProcess = Get-Process "Docker Desktop" -ErrorAction SilentlyContinue
if ($dockerProcess) {
    Write-Host "✓ Docker Desktop 進程存在，但可能還在啟動中" -ForegroundColor Yellow
    Write-Host "  請等待 30-60 秒後重新執行此腳本" -ForegroundColor Yellow
    Write-Host "  或檢查 Docker Desktop 視窗是否正常顯示" -ForegroundColor Yellow
} else {
    Write-Host "✗ Docker Desktop 進程不存在" -ForegroundColor Red
}

# 嘗試啟動 Docker Desktop
Write-Host ""
Write-Host "[4/4] 嘗試啟動 Docker Desktop..." -ForegroundColor Yellow
try {
    Write-Host "  正在啟動 Docker Desktop..." -ForegroundColor Cyan
    Start-Process $dockerPath
    Write-Host "✓ 已啟動 Docker Desktop" -ForegroundColor Green
    Write-Host ""
    Write-Host "請等待 30-60 秒讓 Docker Desktop 完全啟動" -ForegroundColor Yellow
    Write-Host "然後重新執行此腳本或執行: .\啟動Docker服務.ps1" -ForegroundColor Yellow
} catch {
    Write-Host "✗ 無法啟動 Docker Desktop: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "請手動啟動 Docker Desktop：" -ForegroundColor Yellow
    Write-Host "  1. 在開始選單搜尋 'Docker Desktop'" -ForegroundColor Cyan
    Write-Host "  2. 點擊啟動" -ForegroundColor Cyan
    Write-Host "  3. 等待 Docker Desktop 完全啟動（系統托盤會顯示圖標）" -ForegroundColor Cyan
    Write-Host "  4. 然後重新執行此腳本" -ForegroundColor Cyan
}

Write-Host ""



