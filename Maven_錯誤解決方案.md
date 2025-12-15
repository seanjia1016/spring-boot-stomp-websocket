# Maven 錯誤解決方案

## 問題分析

執行 IntelliJ IDEA 內建的 Maven 時出現錯誤：
```
ERROR] Unknown lifecycle phase ".version=2025.2"
```

### 原因
IntelliJ IDEA 內建的 Maven (`plugins/maven/lib/maven3/bin/mvn.cmd`) 在解析參數時，將 `-Didea.version=2025.2` 誤解析為生命週期階段，而不是系統屬性。這是 IntelliJ IDEA 內建 Maven 的參數處理問題。

## 解決方案

### 方案 1：在 IntelliJ IDEA 中改用系統的 Maven（推薦）

1. **打開 IntelliJ IDEA 設置**
   - `File` → `Settings`（或 `Ctrl + Alt + S`）

2. **配置 Maven**
   - 導航至：`Build, Execution, Deployment` → `Build Tools` → `Maven`
   - **Maven home directory**：選擇 `C:\Program Files\Apache\maven`（系統安裝的 Maven）
   - 不要選擇 `Bundled (Maven 3)`

3. **配置 Maven Runner**
   - 在同一個設置頁面，點擊 `Runner`
   - **JRE**：選擇 `1.8` 或 `Use project JDK`
   - **VM options**：輸入 `-Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8`

4. **應用並重新載入項目**
   - 點擊 `Apply` 和 `OK`
   - 右鍵點擊 `pom.xml` → `Maven` → `Reload Project`

### 方案 2：使用命令行執行 Maven（臨時解決方案）

如果需要在命令行執行，使用系統的 Maven：

```powershell
mvn clean -DskipTests=true
```

或使用完整路徑：
```powershell
& "C:\Program Files\Apache\maven\bin\mvn.cmd" clean -DskipTests=true
```

### 方案 3：檢查 IntelliJ IDEA 版本

如果問題持續存在，可能是 IntelliJ IDEA 2025.2 的 bug。可以：
1. 更新到最新版本
2. 或回報給 JetBrains

## 驗證

執行以下命令驗證配置：
```powershell
mvn clean compile -DskipTests
```

如果顯示 `BUILD SUCCESS`，表示配置正確。

## 當前配置狀態

✅ **已完成的配置：**
- `.idea/workspace.xml` - MavenRunner 已配置使用 Java 8
- `pom.xml` - Java 版本設置為 1.8
- `.mvn/jvm.config` - Maven JVM 參數配置
- `.mvn/maven.config` - Maven 編譯器配置

⚠️ **需要手動操作：**
- 在 IntelliJ IDEA 設置中將 Maven home 改為系統的 Maven（方案 1）

