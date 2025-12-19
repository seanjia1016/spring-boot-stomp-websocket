# 實時監控私信相關的日誌
Write-Host "=== 實時監控私信日誌 ===" -ForegroundColor Green
Write-Host "按 Ctrl+C 停止監控`n" -ForegroundColor Yellow

$logFile = "logs\spring-boot-stomp.log"
$patterns = @("私信", "privateMessage", "用戶ID", "登入用戶", "=== 私信發送調試 ===", "發送者ID", "接收者ID", "目標路徑", "私信已發送")

if (-not (Test-Path $logFile)) {
    Write-Host "日誌文件不存在: $logFile" -ForegroundColor Red
    exit
}

# 獲取當前文件大小
$lastSize = (Get-Item $logFile).Length

while ($true) {
    Start-Sleep -Milliseconds 500
    
    if (Test-Path $logFile) {
        $currentSize = (Get-Item $logFile).Length
        
        if ($currentSize -gt $lastSize) {
            # 讀取新增的內容
            $stream = [System.IO.File]::Open($logFile, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::ReadWrite)
            $stream.Position = $lastSize
            $reader = New-Object System.IO.StreamReader($stream)
            $newContent = $reader.ReadToEnd()
            $reader.Close()
            $stream.Close()
            
            if ($newContent) {
                # 檢查是否包含關鍵字
                $lines = $newContent -split "`n"
                foreach ($line in $lines) {
                    foreach ($pattern in $patterns) {
                        if ($line -match $pattern) {
                            Write-Host $line -ForegroundColor Cyan
                            break
                        }
                    }
                }
            }
            
            $lastSize = $currentSize
        }
    }
}








