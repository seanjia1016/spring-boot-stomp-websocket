# 自動清理 Java Language Server 工作區快取
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "自動清理 Java Language Server 快取" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Java Language Server 工作區路徑
$workspacePath = "$env:APPDATA\Cursor\User\workspaceStorage"
$javaLsWorkspace = "$env:APPDATA\Cursor\User\globalStorage\redhat.java"

Write-Host "正在檢查 Java Language Server 工作區..." -ForegroundColor Yellow

# 檢查並列出工作區
if (Test-Path $workspacePath) {
    $workspaces = Get-ChildItem $workspacePath -Directory | Where-Object { 
        $_.Name -match ".*" 
    }
    Write-Host "找到 $($workspaces.Count) 個工作區" -ForegroundColor Gray
    
    foreach ($ws in $workspaces) {
        $javaLsCache = Join-Path $ws.FullName "redhat.java"
        if (Test-Path $javaLsCache) {
            Write-Host "  工作區: $($ws.Name)" -ForegroundColor Gray
            Write-Host "    Java LS 快取: $javaLsCache" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "工作區目錄不存在: $workspacePath" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "正在檢查 Java Language Server 全域快取..." -ForegroundColor Yellow

if (Test-Path $javaLsWorkspace) {
    $items = Get-ChildItem $javaLsWorkspace -Directory
    Write-Host "找到 $($items.Count) 個快取項目" -ForegroundColor Gray
    
    foreach ($item in $items) {
        Write-Host "  - $($item.Name)" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "注意：為了安全起見，不會自動刪除這些快取。" -ForegroundColor Yellow
    Write-Host "請在 Cursor 中使用命令面板清理：" -ForegroundColor Yellow
    Write-Host "  Java: Clean Java Language Server Workspace" -ForegroundColor White
} else {
    Write-Host "Java Language Server 全域快取目錄不存在" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "建議操作：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 在 Cursor 中按 Ctrl + Shift + P" -ForegroundColor White
Write-Host "2. 輸入: Developer: Reload Window" -ForegroundColor White
Write-Host "3. 或輸入: Java: Clean Java Language Server Workspace" -ForegroundColor White
Write-Host "4. 選擇: Restart and delete" -ForegroundColor White
Write-Host ""
Write-Host "這樣會自動清理快取並重新啟動 Language Server" -ForegroundColor Gray
Write-Host ""















