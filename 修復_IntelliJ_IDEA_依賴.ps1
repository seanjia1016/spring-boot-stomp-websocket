# IntelliJ IDEA 依賴問題修復腳本
# 此腳本會刪除 .idea 和 .iml 文件，讓 IntelliJ IDEA 重新導入專案

Write-Host "`n=== IntelliJ IDEA 依賴問題修復腳本 ===" -ForegroundColor Cyan
Write-Host "`n此腳本將：" -ForegroundColor Yellow
Write-Host "  1. 備份 .idea 資料夾（如果存在）" -ForegroundColor White
Write-Host "  2. 刪除 .idea 資料夾" -ForegroundColor White
Write-Host "  3. 刪除 spring-boot-stomp.iml 文件" -ForegroundColor White
Write-Host "`n警告: 這會刪除所有 IntelliJ IDEA 的專案設置！" -ForegroundColor Red
Write-Host "但不會影響您的代碼文件。" -ForegroundColor Green

$confirm = Read-Host "`n是否繼續？(Y/N)"

if ($confirm -ne "Y" -and $confirm -ne "y") {
    Write-Host "`n操作已取消。" -ForegroundColor Yellow
    exit
}

# 檢查 IntelliJ IDEA 是否正在運行
$ideaProcesses = Get-Process -Name "idea64" -ErrorAction SilentlyContinue
if ($ideaProcesses) {
    Write-Host "`n警告: 檢測到 IntelliJ IDEA 正在運行！" -ForegroundColor Red
    Write-Host "請先關閉 IntelliJ IDEA，然後再運行此腳本。" -ForegroundColor Yellow
    exit
}

# 備份 .idea 資料夾
if (Test-Path ".idea") {
    $backupPath = ".idea.backup.$(Get-Date -Format 'yyyyMMdd_HHmmss')"
    Write-Host "`n正在備份 .idea 資料夾到: $backupPath" -ForegroundColor Yellow
    Copy-Item -Path ".idea" -Destination $backupPath -Recurse -Force
    Write-Host "備份完成！" -ForegroundColor Green
}

# 刪除 .idea 資料夾
if (Test-Path ".idea") {
    Write-Host "`n正在刪除 .idea 資料夾..." -ForegroundColor Yellow
    Remove-Item -Path ".idea" -Recurse -Force
    Write-Host ".idea 資料夾已刪除！" -ForegroundColor Green
}

# 刪除 .iml 文件
$imlFiles = Get-ChildItem -Path "." -Filter "*.iml" -ErrorAction SilentlyContinue
foreach ($imlFile in $imlFiles) {
    Write-Host "`n正在刪除 $($imlFile.Name)..." -ForegroundColor Yellow
    Remove-Item -Path $imlFile.FullName -Force
    Write-Host "$($imlFile.Name) 已刪除！" -ForegroundColor Green
}

Write-Host "`n=== 修復完成 ===" -ForegroundColor Green
Write-Host "`n下一步操作：" -ForegroundColor Cyan
Write-Host "  1. 打開 IntelliJ IDEA" -ForegroundColor White
Write-Host "  2. File → Open" -ForegroundColor White
Write-Host "  3. 選擇此專案目錄（包含 pom.xml 的目錄）" -ForegroundColor White
Write-Host "  4. 選擇 'Import project from external model' → 'Maven'" -ForegroundColor White
Write-Host "  5. 按照嚮導完成導入，確保選擇 Java 17 作為 SDK" -ForegroundColor White
Write-Host "  6. 等待 Maven 專案載入完成" -ForegroundColor White
Write-Host "`n完成後，所有依賴應該都能正常使用了！" -ForegroundColor Green

