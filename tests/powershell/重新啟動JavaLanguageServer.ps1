# 重新啟動 Java Language Server 腳本
# 此腳本會清理 Java Language Server 的工作區快取

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Java Language Server 重新啟動工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 檢查 Java 17 配置
$java17Path = "C:\Infotrends\Info360\azul-17"
if (Test-Path $java17Path) {
    Write-Host "✓ Java 17 路徑存在: $java17Path" -ForegroundColor Green
    $javaVersion = & "$java17Path\bin\java.exe" -version 2>&1 | Select-Object -First 1
    Write-Host "  版本: $javaVersion" -ForegroundColor Gray
} else {
    Write-Host "✗ Java 17 路徑不存在: $java17Path" -ForegroundColor Red
}

Write-Host ""

# 檢查設定檔
$settingsPath = ".vscode\settings.json"
if (Test-Path $settingsPath) {
    Write-Host "✓ 設定檔存在: $settingsPath" -ForegroundColor Green
    $settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
    if ($settings.'java.home' -eq $java17Path) {
        Write-Host "  Java Home 配置正確" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Java Home 配置可能不正確" -ForegroundColor Yellow
    }
} else {
    Write-Host "✗ 設定檔不存在: $settingsPath" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "請在 Cursor 中執行以下操作：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "方法 1：重新載入視窗（推薦）" -ForegroundColor Cyan
Write-Host "  1. 按 Ctrl + Shift + P" -ForegroundColor White
Write-Host "  2. 輸入: Developer: Reload Window" -ForegroundColor White
Write-Host "  3. 按 Enter" -ForegroundColor White
Write-Host ""
Write-Host "方法 2：清理並重啟 Language Server" -ForegroundColor Cyan
Write-Host "  1. 按 Ctrl + Shift + P" -ForegroundColor White
Write-Host "  2. 輸入: Java: Clean Java Language Server Workspace" -ForegroundColor White
Write-Host "  3. 選擇: Restart and delete" -ForegroundColor White
Write-Host ""
Write-Host "方法 3：完全重啟 Cursor" -ForegroundColor Cyan
Write-Host "  直接關閉並重新開啟 Cursor" -ForegroundColor White
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "驗證設定是否成功：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "重啟後，檢查以下項目：" -ForegroundColor White
Write-Host "  ✓ 右下角不再顯示 Java Language Server 錯誤" -ForegroundColor Gray
Write-Host "  ✓ Java 檔案有語法高亮顯示" -ForegroundColor Gray
Write-Host "  ✓ 可以正常使用自動完成功能" -ForegroundColor Gray
Write-Host "  ✓ 沒有 Java 8 相關的錯誤訊息" -ForegroundColor Gray
Write-Host ""
Write-Host "如果仍有問題，請檢查：" -ForegroundColor Yellow
Write-Host "  1. 輸出面板中的 Java Language Server 日誌" -ForegroundColor Gray
Write-Host "  2. 確認 Java 17 路徑是否正確" -ForegroundColor Gray
Write-Host "  3. 檢查 .vscode/settings.json 配置" -ForegroundColor Gray
Write-Host ""



