# Maven 服務器錯誤解決方案

## 錯誤訊息
```
錯誤: 找不到或無法載入主要類別 org.jetbrains.idea.maven.server.RemoteMavenServer36
```

## 原因
IntelliJ IDEA 內建的 Maven 服務器與當前 Java 版本不兼容，或 Maven 服務器配置有問題。

## 解決方案

### 方案 1：改用系統的 Maven（推薦）

1. **打開 IntelliJ IDEA 設置**
   - `File` → `Settings`（或 `Ctrl + Alt + S`）

2. **配置 Maven**
   - 導航至：`Build, Execution, Deployment` → `Build Tools` → `Maven`
   - **Maven home directory**：
     - 點擊下拉選單
     - 選擇 `C:\Program Files\Apache\maven`（系統安裝的 Maven）
     - **不要選擇** `Bundled (Maven 3)` 或 `Use plugin registry`
   - **User settings file**：確認指向正確的 `settings.xml`（通常為 `C:\Users\您的用戶名\.m2\settings.xml`）

3. **配置 Maven Runner**
   - 在同一個設置頁面，點擊 **Runner** 標籤
   - **JRE**：選擇 **1.8** 或 **Use project JDK**
   - **VM options**：輸入 `-Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8`
   - 勾選 **Use project settings**

4. **應用設置**
   - 點擊 **Apply** 和 **OK**

5. **重新載入 Maven 專案**
   - 右鍵點擊 `pom.xml`
   - 選擇 **Maven** → **Reload Project**
   - 或點擊 Maven 工具視窗中的重新載入按鈕

### 方案 2：重置 Maven 設置

如果方案 1 不行，嘗試重置：

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven**
2. 將 **Maven home directory** 改回 `Bundled (Maven 3)`
3. 點擊 **Apply**
4. 再改回系統的 Maven（`C:\Program Files\Apache\maven`）
5. 點擊 **Apply** 和 **OK**
6. 重新載入 Maven 專案

### 方案 3：檢查 Java SDK 配置

1. **File** → **Project Structure** → **SDKs**
2. 確認 Java 8 SDK 已正確配置
3. 如果沒有，點擊 **+** 添加：
   - 選擇 **JDK**
   - 路徑：`C:\Program Files\Zulu\zulu-8`
   - 點擊 **OK**

4. **File** → **Project Structure** → **Project**
   - **Project SDK**：選擇 **1.8**
   - 點擊 **Apply** 和 **OK**

### 方案 4：手動重啟 Maven 服務器

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Runner**
2. 取消勾選 **Use project settings**（臨時）
3. 點擊 **Apply**
4. 再勾選回 **Use project settings**
5. 點擊 **Apply** 和 **OK**
6. 重新載入 Maven 專案

## 驗證

完成設置後：
1. 打開 **Maven** 工具視窗（右側邊欄）
2. 應該能看到專案的 Maven 結構
3. 執行 **Reload Project** 應該不會再出現錯誤

## 如果問題仍然存在

1. **關閉 IntelliJ IDEA**
2. **刪除 Maven 本地倉庫索引**（可選）：
   - 刪除 `C:\Users\您的用戶名\.IntelliJIdea2025.2\system\Maven\Indices`
3. **重新打開 IntelliJ IDEA**
4. **重新配置 Maven 設置**（使用方案 1）

## 注意事項

- **必須使用系統的 Maven**，不要使用 IntelliJ IDEA 內建的 Maven
- 確保 Maven Runner 的 JRE 設置為 Java 8
- 如果修改了設置，記得點擊 **Apply** 和 **OK** 保存

