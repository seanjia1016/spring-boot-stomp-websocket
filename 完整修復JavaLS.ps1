# 完整修復 Java Language Server 配置
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "完整修復 Java Language Server 配置" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$java17Path = "C:\Infotrends\Info360\azul-17"

# 1. 設置用戶級 JAVA_HOME
Write-Host "步驟 1: 設置用戶級 JAVA_HOME..." -ForegroundColor Yellow
try {
    [Environment]::SetEnvironmentVariable("JAVA_HOME", $java17Path, "User")
    Write-Host "✓ 已設置用戶級 JAVA_HOME = $java17Path" -ForegroundColor Green
} catch {
    Write-Host "✗ 設置 JAVA_HOME 失敗: $_" -ForegroundColor Red
}

Write-Host ""

# 2. 驗證 Java 17
Write-Host "步驟 2: 驗證 Java 17 安裝..." -ForegroundColor Yellow
if (Test-Path "$java17Path\bin\java.exe") {
    $version = & "$java17Path\bin\java.exe" -version 2>&1 | Select-Object -First 1
    Write-Host "✓ Java 17 可用: $version" -ForegroundColor Green
} else {
    Write-Host "✗ Java 17 路徑不存在" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 3. 檢查設定檔
Write-Host "步驟 3: 檢查設定檔..." -ForegroundColor Yellow
$settingsPath = ".vscode\settings.json"
if (Test-Path $settingsPath) {
    $settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
    if ($settings.'java.home' -eq $java17Path) {
        Write-Host "✓ 設定檔配置正確" -ForegroundColor Green
    } else {
        Write-Host "⚠ 設定檔配置可能需要更新" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ 設定檔不存在" -ForegroundColor Red
}

Write-Host ""

# 4. 顯示需要手動執行的步驟
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "重要：需要重新啟動 Cursor 才能套用環境變數變更" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "請執行以下步驟：" -ForegroundColor White
Write-Host ""
Write-Host "1. 完全關閉 Cursor（確保所有視窗都關閉）" -ForegroundColor Cyan
Write-Host "2. 重新開啟 Cursor" -ForegroundColor Cyan
Write-Host "3. 開啟專案後，按 Ctrl + Shift + P" -ForegroundColor Cyan
Write-Host "4. 輸入: Java: Clean Java Language Server Workspace" -ForegroundColor Cyan
Write-Host "5. 選擇: Restart and delete" -ForegroundColor Cyan
Write-Host ""
Write-Host "或者，如果上述方法不行：" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. 完全關閉 Cursor" -ForegroundColor Cyan
Write-Host "2. 刪除以下目錄（可選，會清除所有 Java LS 快取）：" -ForegroundColor Cyan
Write-Host "   C:\Users\賈其翔\AppData\Roaming\Cursor\User\globalStorage\redhat.java" -ForegroundColor Yellow
Write-Host "3. 重新開啟 Cursor" -ForegroundColor Cyan
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "驗證步驟：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "重啟後，檢查：" -ForegroundColor White
Write-Host "  ✓ 輸出面板中沒有 Java 8 相關錯誤" -ForegroundColor Gray
Write-Host "  ✓ Java 檔案有語法高亮" -ForegroundColor Gray
Write-Host "  ✓ 自動完成功能正常" -ForegroundColor Gray
Write-Host ""

