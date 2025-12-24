# Windows PowerShell 命令對照表

## 說明

在 Windows PowerShell 環境下，某些 Linux/Mac 的命令無法直接使用。本文檔提供 Windows PowerShell 的對應命令。

---

## Docker 相關命令

### 檢查 Redis 容器是否運行

**Linux/Mac：**
```bash
docker ps | grep redis
```

**Windows PowerShell：**
```powershell
# 方法1：使用 Select-String（推薦）
docker ps | Select-String redis

# 方法2：使用 Docker 過濾器（最簡潔）
docker ps --filter "name=redis"

# 方法3：直接查看所有容器
docker ps
```

**說明**：
- `grep` 是 Linux/Mac 的命令，Windows PowerShell 不支援
- `Select-String` 是 PowerShell 的對應命令
- `--filter` 是 Docker 的原生命令，所有平台都支援

---

## HTTP 請求命令

### 測試 API 端點

**Linux/Mac：**
```bash
curl http://localhost:8080/api/agent/a
```

**Windows PowerShell：**
```powershell
# 方法1：使用 Invoke-WebRequest（推薦）
Invoke-WebRequest -Uri "http://localhost:8080/api/agent/a" | Select-Object -ExpandProperty Content

# 方法2：簡化版本（只獲取內容）
(Invoke-WebRequest -Uri "http://localhost:8080/api/agent/a").Content

# 方法3：如果已安裝 curl（Windows 10 1803+）
curl http://localhost:8080/api/agent/a
```

**說明**：
- Windows 10 1803 及更高版本內建 `curl`，但功能可能不如 Linux 版本完整
- `Invoke-WebRequest` 是 PowerShell 的原生命令，功能更強大

### 格式化 JSON 回應

**Windows PowerShell：**
```powershell
# 獲取並格式化 JSON
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/agent/a"
$json = $response.Content | ConvertFrom-Json
$json | ConvertTo-Json -Depth 10
```

---

## 文件操作命令

### 查看文件內容

**Linux/Mac：**
```bash
cat file.txt
head -n 20 file.txt
tail -n 20 file.txt
```

**Windows PowerShell：**
```powershell
# 查看完整文件
Get-Content file.txt

# 查看前 20 行
Get-Content file.txt -TotalCount 20

# 查看後 20 行
Get-Content file.txt -Tail 20
```

### 搜尋文件內容

**Linux/Mac：**
```bash
grep "pattern" file.txt
grep -r "pattern" directory/
```

**Windows PowerShell：**
```powershell
# 搜尋文件內容
Select-String -Pattern "pattern" file.txt

# 遞迴搜尋目錄
Select-String -Pattern "pattern" -Path "directory\" -Recursive
```

---

## 進程管理命令

### 查看 Java 進程

**Linux/Mac：**
```bash
ps aux | grep java
```

**Windows PowerShell：**
```powershell
# 查看所有 Java 進程
Get-Process -Name "java" -ErrorAction SilentlyContinue

# 查看詳細資訊
Get-Process -Name "java" | Format-Table -AutoSize
```

### 停止 Java 進程

**Linux/Mac：**
```bash
kill -9 <PID>
```

**Windows PowerShell：**
```powershell
# 停止指定 PID 的進程
Stop-Process -Id <PID> -Force

# 停止所有 Java 進程
Get-Process -Name "java" | Stop-Process -Force
```

---

## 環境變數命令

### 查看環境變數

**Linux/Mac：**
```bash
echo $JAVA_HOME
env | grep JAVA
```

**Windows PowerShell：**
```powershell
# 查看環境變數
$env:JAVA_HOME

# 查看所有環境變數
Get-ChildItem Env: | Where-Object { $_.Name -like "*JAVA*" }
```

---

## 網路相關命令

### 檢查端口是否被占用

**Linux/Mac：**
```bash
netstat -ano | grep 8080
lsof -i :8080
```

**Windows PowerShell：**
```powershell
# 檢查端口是否被占用
netstat -ano | Select-String "8080"

# 查看詳細資訊
Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
```

---

## 常用 PowerShell 別名

PowerShell 提供了一些別名，讓 Linux 用戶更容易適應：

| Linux 命令 | PowerShell 別名 | PowerShell 完整命令 |
|-----------|---------------|-------------------|
| `ls` | `ls` | `Get-ChildItem` |
| `cat` | `cat` | `Get-Content` |
| `pwd` | `pwd` | `Get-Location` |
| `cd` | `cd` | `Set-Location` |
| `rm` | `rm` | `Remove-Item` |
| `cp` | `cp` | `Copy-Item` |
| `mv` | `mv` | `Move-Item` |

**注意**：雖然有別名，但功能可能不完全相同，建議學習 PowerShell 的原生命令。

---

## 實用 PowerShell 技巧

### 1. 管道操作

```powershell
# 過濾並格式化輸出
docker ps | Where-Object { $_.Contains("redis") } | Format-Table
```

### 2. 變數使用

```powershell
# 儲存命令結果到變數
$redisContainer = docker ps | Select-String redis
$redisContainer
```

### 3. 條件判斷

```powershell
# 檢查 Redis 是否運行
$redisRunning = docker ps | Select-String redis
if ($redisRunning) {
    Write-Host "Redis 正在運行" -ForegroundColor Green
} else {
    Write-Host "Redis 未運行" -ForegroundColor Red
}
```

---

## 總結

在 Windows 環境下開發時：

1. **優先使用 PowerShell 原生命令**：功能更強大，更符合 Windows 生態
2. **使用 Docker 原生過濾器**：跨平台兼容，無需修改
3. **學習 PowerShell 語法**：雖然有別名，但原生命令更可靠
4. **使用 WSL（可選）**：如果需要完整的 Linux 環境，可以使用 Windows Subsystem for Linux

---

**文檔版本**：1.0  
**最後更新**：2025-12-19









