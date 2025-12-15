# IntelliJ IDEA 依賴問題解決方案

## 問題
IntelliJ IDEA 編譯時找不到 Spring 和 Lombok 的依賴，但 Maven 命令行可以正常編譯。

## 原因
IntelliJ IDEA 沒有正確載入 Maven 依賴到模組配置中。

## 解決方案

### 方案 1：重新載入 Maven 專案（最簡單）

1. **右鍵點擊 `pom.xml`**
2. 選擇 **Maven** → **Reload Project**
3. 等待 Maven 專案重新載入完成
4. 檢查 **Maven** 工具視窗，確認所有依賴都已載入

### 方案 2：使緩存失效並重啟（如果方案 1 不行）

1. **File** → **Invalidate Caches / Restart...**
2. 選擇 **Invalidate and Restart**
3. 等待 IntelliJ IDEA 完全重啟
4. 重新載入 Maven 專案

### 方案 3：檢查 Maven 設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Importing**
2. 確認以下設置：
   - ✅ **Import Maven projects automatically**
   - ✅ **Automatically download**: Sources and Documentation
   - ✅ **Use Maven wrapper** (如果有的話)
3. 點擊 **Apply** 和 **OK**
4. 重新載入 Maven 專案

### 方案 4：手動重新導入專案

1. **File** → **Close Project**
2. **File** → **Open**
3. 選擇專案根目錄（包含 `pom.xml` 的目錄）
4. 選擇 **Open as Project**
5. 如果提示，選擇 **Import project from external model** → **Maven**
6. 按照嚮導完成導入

### 方案 5：檢查專案結構

1. 按 `Ctrl + Alt + Shift + S` 打開 **Project Structure**
2. **Modules** 標籤頁：
   - 確認 `spring-boot-stomp` 模組存在
   - 檢查 **Dependencies** 標籤，應該能看到所有 Maven 依賴
3. 如果沒有依賴，點擊 **+** → **Library** → **From Maven...**
4. 重新載入 Maven 專案

## 驗證

完成後檢查：
1. **Maven** 工具視窗應該顯示所有依賴
2. 代碼中的導入語句不應該有紅色錯誤
3. **Build** → **Rebuild Project** 應該成功

## 如果問題仍然存在

1. **檢查 Maven 本地倉庫**：
   - 確認 `C:\Users\您的用戶名\.m2\repository` 中有依賴文件
   - 如果沒有，執行 `mvn clean install -U`

2. **檢查 Java SDK**：
   - **File** → **Project Structure** → **Project**
   - **Project SDK**: 應該是 **17**
   - **Project language level**: 應該是 **17**

3. **檢查編譯器設置**：
   - **File** → **Settings** → **Build, Execution, Deployment** → **Compiler** → **Java Compiler**
   - **Project bytecode version**: 應該是 **17**

