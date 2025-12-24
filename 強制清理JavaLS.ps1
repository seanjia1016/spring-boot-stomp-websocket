# 強制清理 Java Language Server 快取
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "強制清理 Java Language Server 快取" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$workspacePath = "$env:APPDATA\Cursor\User\workspaceStorage"
$javaLsGlobal = "$env:APPDATA\Cursor\User\globalStorage\redhat.java"
$currentWorkspace = (Get-Location).Path

Write-Host "當前工作區: $currentWorkspace" -ForegroundColor Yellow
Write-Host ""

# 計算當前工作區的哈希值（VS Code/Cursor 使用路徑哈希）
$workspaceHash = [System.Security.Cryptography.MD5]::Create().ComputeHash([System.Text.Encoding]::UTF8.GetBytes($currentWorkspace.ToLower()))
$workspaceHashString = ($workspaceHash | ForEach-Object { $_.ToString("x2") }) -join ""
Write-Host "工作區哈希: $workspaceHashString" -ForegroundColor Gray
Write-Host ""

# 查找並清理當前工作區的快取
Write-Host "正在查找當前工作區的快取..." -ForegroundColor Yellow
$found = $false

if (Test-Path $workspacePath) {
    $workspaces = Get-ChildItem $workspacePath -Directory
    foreach ($ws in $workspaces) {
        $javaLsCache = Join-Path $ws.FullName "redhat.java"
        if (Test-Path $javaLsCache) {
            Write-Host "找到 Java LS 快取: $javaLsCache" -ForegroundColor Gray
            
            # 檢查是否包含 Java 8 相關的配置
            $configFile = Join-Path $javaLsCache "config_win"
            if (Test-Path $configFile) {
                Write-Host "  發現配置目錄: $configFile" -ForegroundColor Yellow
                
                # 查找並顯示可能的問題配置
                $xmlFiles = Get-ChildItem $configFile -Recurse -Filter "*.xml" -ErrorAction SilentlyContinue
                foreach ($xml in $xmlFiles) {
                    $content = Get-Content $xml.FullName -Raw -ErrorAction SilentlyContinue
                    if ($content -match "zulu-8" -or $content -match "Java.*8") {
                        Write-Host "    ⚠ 發現 Java 8 配置: $($xml.FullName)" -ForegroundColor Red
                    }
                }
            }
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "建議的清理步驟：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "1. 在 Cursor 中執行以下命令：" -ForegroundColor White
Write-Host "   Ctrl + Shift + P" -ForegroundColor Gray
Write-Host "   → Java: Clean Java Language Server Workspace" -ForegroundColor Gray
Write-Host "   → 選擇: Restart and delete" -ForegroundColor Gray
Write-Host ""
Write-Host "2. 如果還是不行，可以手動刪除以下目錄（需要關閉 Cursor）：" -ForegroundColor White
Write-Host "   $javaLsGlobal" -ForegroundColor Yellow
Write-Host ""
Write-Host "3. 然後重新啟動 Cursor" -ForegroundColor White
Write-Host ""

# 檢查環境變數
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "檢查環境變數：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$javaHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
if ($javaHome) {
    Write-Host "JAVA_HOME (User): $javaHome" -ForegroundColor Gray
    if ($javaHome -match "zulu-8" -or $javaHome -match "java.*8") {
        Write-Host "  ⚠ JAVA_HOME 指向 Java 8，可能導致問題" -ForegroundColor Red
        Write-Host "  建議設置為: C:\Infotrends\Info360\azul-17" -ForegroundColor Yellow
    } elseif ($javaHome -match "azul-17") {
        Write-Host "  ✓ JAVA_HOME 指向 Java 17" -ForegroundColor Green
    }
} else {
    Write-Host "JAVA_HOME (User): 未設置" -ForegroundColor Yellow
}

$javaHomeMachine = [Environment]::GetEnvironmentVariable("JAVA_HOME", "Machine")
if ($javaHomeMachine) {
    Write-Host "JAVA_HOME (Machine): $javaHomeMachine" -ForegroundColor Gray
    if ($javaHomeMachine -match "zulu-8" -or $javaHomeMachine -match "java.*8") {
        Write-Host "  ⚠ 系統 JAVA_HOME 指向 Java 8" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "檢查 PATH 環境變數：" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$path = [Environment]::GetEnvironmentVariable("PATH", "User")
$pathParts = $path -split ";"
$java8InPath = $pathParts | Where-Object { $_ -match "zulu-8" -or $_ -match "java.*8" }
$java17InPath = $pathParts | Where-Object { $_ -match "azul-17" }

if ($java8InPath) {
    Write-Host "⚠ PATH 中包含 Java 8 路徑：" -ForegroundColor Red
    foreach ($p in $java8InPath) {
        Write-Host "  - $p" -ForegroundColor Gray
    }
}

if ($java17InPath) {
    Write-Host "✓ PATH 中包含 Java 17 路徑：" -ForegroundColor Green
    foreach ($p in $java17InPath) {
        Write-Host "  - $p" -ForegroundColor Gray
    }
}

Write-Host ""













