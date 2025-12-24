# 專員ID變更自動化測試腳本（自動修正、重啟、再測試）
# 此腳本會自動執行測試，如果失敗則修正、重啟、再測試，直到成功為止

$ErrorActionPreference = "Continue"
$maxRetries = 10
$retryCount = 0
$testPassed = $false

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "專員ID變更自動化測試（自動修正模式）" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

function Test-ApplicationRunning {
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080" -Method GET -TimeoutSec 2 -ErrorAction Stop
        return $true
    } catch {
        return $false
    }
}

function Start-Application {
    Write-Host "啟動應用程式..." -ForegroundColor Yellow
    
    # 停止現有的Java進程
    $javaProcesses = Get-Process -Name "java" -ErrorAction SilentlyContinue
    if ($javaProcesses) {
        Write-Host "停止現有Java進程..." -ForegroundColor Cyan
        $javaProcesses | ForEach-Object {
            Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
        }
        Start-Sleep -Seconds 2
    }
    
    # 編譯並啟動
    Write-Host "編譯應用程式..." -ForegroundColor Cyan
    mvn clean compile -q 2>&1 | Out-Null
    
    if ($LASTEXITCODE -ne 0) {
        Write-Host "✗ 編譯失敗" -ForegroundColor Red
        return $false
    }
    
    Write-Host "啟動應用程式（背景運行）..." -ForegroundColor Cyan
    $process = Start-Process -FilePath "mvn" -ArgumentList "spring-boot:run" -PassThru -WindowStyle Hidden
    
    # 等待應用程式啟動
    Write-Host "等待應用程式啟動..." -ForegroundColor Yellow
    $timeout = 60
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        if (Test-ApplicationRunning) {
            Write-Host "✓ 應用程式已啟動" -ForegroundColor Green
            Start-Sleep -Seconds 3  # 額外等待，確保完全啟動
            return $true
        }
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host "." -NoNewline -ForegroundColor Gray
    }
    
    Write-Host "`n✗ 應用程式啟動超時" -ForegroundColor Red
    return $false
}

function Start-DockerServices {
    Write-Host "啟動Docker服務..." -ForegroundColor Yellow
    
    # 檢查Docker是否運行
    try {
        docker ps 2>&1 | Out-Null
        if ($LASTEXITCODE -ne 0) {
            Write-Host "啟動Docker Desktop..." -ForegroundColor Cyan
            Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 10
        }
    } catch {
        Write-Host "啟動Docker Desktop..." -ForegroundColor Cyan
        Start-Process "C:\Program Files\Docker\Docker\Docker Desktop.exe" -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 10
    }
    
    # 啟動Docker服務
    docker-compose up -d 2>&1 | Out-Null
    
    # 等待服務啟動
    Write-Host "等待Docker服務啟動..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    Write-Host "✓ Docker服務已啟動" -ForegroundColor Green
}

function Run-Test {
    Write-Host "`n執行測試..." -ForegroundColor Yellow
    
    # 檢查Node.js是否安裝
    try {
        $nodeVersion = node --version 2>&1
        Write-Host "Node.js版本: $nodeVersion" -ForegroundColor Cyan
    } catch {
        Write-Host "✗ Node.js未安裝，請先安裝Node.js" -ForegroundColor Red
        return $false
    }
    
    # 檢查測試腳本依賴
    $testDir = "tests\javascript"
    if (-not (Test-Path "$testDir\package.json")) {
        Write-Host "初始化Node.js專案..." -ForegroundColor Cyan
        Push-Location $testDir
        npm init -y 2>&1 | Out-Null
        npm install sockjs-client stompjs --save 2>&1 | Out-Null
        Pop-Location
    } else {
        # 檢查依賴是否已安裝
        if (-not (Test-Path "$testDir\node_modules")) {
            Write-Host "安裝測試依賴..." -ForegroundColor Cyan
            Push-Location $testDir
            npm install 2>&1 | Out-Null
            Pop-Location
        }
    }
    
    # 執行測試
    Write-Host "運行測試腳本..." -ForegroundColor Cyan
    Push-Location $testDir
    node 專員ID變更完整測試.js
    $testResult = $LASTEXITCODE
    Pop-Location
    
    return $testResult -eq 0
}

function Fix-CommonIssues {
    Write-Host "`n檢查並修正常見問題..." -ForegroundColor Yellow
    
    $fixed = $false
    
    # 檢查Redis連接
    try {
        $redisTest = docker exec redis redis-cli ping 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "修正：重啟Redis容器..." -ForegroundColor Cyan
            docker restart redis 2>&1 | Out-Null
            Start-Sleep -Seconds 3
            $fixed = $true
        }
    } catch {
        Write-Host "修正：啟動Redis容器..." -ForegroundColor Cyan
        docker-compose up -d redis 2>&1 | Out-Null
        Start-Sleep -Seconds 3
        $fixed = $true
    }
    
    # 檢查RabbitMQ連接
    try {
        $rabbitmqTest = docker exec rabbitmq rabbitmq-diagnostics ping 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "修正：重啟RabbitMQ容器..." -ForegroundColor Cyan
            docker restart rabbitmq 2>&1 | Out-Null
            Start-Sleep -Seconds 3
            $fixed = $true
        }
    } catch {
        Write-Host "修正：啟動RabbitMQ容器..." -ForegroundColor Cyan
        docker-compose up -d rabbitmq 2>&1 | Out-Null
        Start-Sleep -Seconds 3
        $fixed = $true
    }
    
    if ($fixed) {
        Write-Host "✓ 已修正問題" -ForegroundColor Green
    } else {
        Write-Host "未發現需要修正的問題" -ForegroundColor Gray
    }
    
    return $fixed
}

# 主循環
while ($retryCount -lt $maxRetries -and -not $testPassed) {
    $retryCount++
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "第 $retryCount 次嘗試" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
    
    # 步驟1：確保Docker服務運行
    Write-Host "步驟1：檢查Docker服務..." -ForegroundColor Cyan
    Start-DockerServices
    
    # 步驟2：確保應用程式運行
    Write-Host "`n步驟2：檢查應用程式..." -ForegroundColor Cyan
    if (-not (Test-ApplicationRunning)) {
        Write-Host "應用程式未運行，正在啟動..." -ForegroundColor Yellow
        if (-not (Start-Application)) {
            Write-Host "✗ 應用程式啟動失敗" -ForegroundColor Red
            Write-Host "等待5秒後重試..." -ForegroundColor Yellow
            Start-Sleep -Seconds 5
            continue
        }
    } else {
        Write-Host "✓ 應用程式正在運行" -ForegroundColor Green
    }
    
    # 步驟3：執行測試
    Write-Host "`n步驟3：執行測試..." -ForegroundColor Cyan
    $testPassed = Run-Test
    
    if ($testPassed) {
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "✓ 測試通過！" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Cyan
        Write-Host "總共嘗試次數: $retryCount" -ForegroundColor Cyan
        break
    } else {
        Write-Host "`n✗ 測試失敗" -ForegroundColor Red
        
        if ($retryCount -lt $maxRetries) {
            Write-Host "`n執行修正和重啟..." -ForegroundColor Yellow
            
            # 修正常見問題
            Fix-CommonIssues
            
            # 重啟應用程式
            Write-Host "`n重啟應用程式..." -ForegroundColor Yellow
            Start-Application
            
            # 等待一下再重試
            Write-Host "等待5秒後重試..." -ForegroundColor Yellow
            Start-Sleep -Seconds 5
        }
    }
}

if (-not $testPassed) {
    Write-Host "`n========================================" -ForegroundColor Cyan
    Write-Host "✗ 測試失敗（已嘗試 $maxRetries 次）" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Cyan
    exit 1
} else {
    exit 0
}







