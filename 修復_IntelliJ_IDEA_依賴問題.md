# 修復 IntelliJ IDEA 依賴問題

## 問題診斷

- ✅ **Maven 命令行**: 測試通過（48 個測試，0 失敗）
- ❌ **IntelliJ IDEA**: 找不到 lombok 和其他 Spring 依賴

**根本原因**: IntelliJ IDEA 沒有正確載入 Maven 依賴到模組配置中。

## 解決方案（按順序執行）

### 步驟 1：重新載入 Maven 專案（必須）

1. **右鍵點擊 `pom.xml`**
2. 選擇 **Maven** → **Reload Project**
3. **等待完成**：觀察 Maven 工具視窗，確認所有依賴都已載入
4. 如果看到進度條，等待它完成

### 步驟 2：檢查 Maven Import 設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Importing**
2. 確認以下設置：
   - ✅ **Import Maven projects automatically**（必須勾選）
   - ✅ **Automatically download**: Sources and Documentation（可選，但建議勾選）
   - ✅ **Use Maven wrapper**（如果有）
3. 點擊 **Apply** 和 **OK**
4. **重新載入 Maven 專案**（步驟 1）

### 步驟 3：使緩存失效並重啟（如果步驟 1 和 2 不行）

1. **File** → **Invalidate Caches / Restart...**
2. 選擇 **Invalidate and Restart**
3. 等待 IntelliJ IDEA 完全重啟
4. **重新載入 Maven 專案**（步驟 1）

### 步驟 4：檢查專案結構（驗證）

1. 按 `Ctrl + Alt + Shift + S` 打開 **Project Structure**
2. **Modules** 標籤頁：
   - 選擇 `spring-boot-stomp` 模組
   - 點擊 **Dependencies** 標籤
   - **應該能看到所有 Maven 依賴**，包括：
     - Spring Boot Starter Web
     - Spring Boot Starter WebSocket
     - Lombok
     - 等等
3. 如果沒有依賴，回到步驟 1 重新載入

### 步驟 5：手動觸發 Maven 導入（最後手段）

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Importing**
2. 取消勾選 **Import Maven projects automatically**
3. 點擊 **Apply**
4. 再勾選回 **Import Maven projects automatically**
5. 點擊 **Apply** 和 **OK**
6. **重新載入 Maven 專案**（步驟 1）

## 驗證修復

完成後檢查：

1. **Maven 工具視窗**（右側邊欄）：
   - 應該顯示專案的 Maven 結構
   - 所有依賴都應該可見

2. **代碼編輯器**：
   - `Message.java` 中的 `import lombok.*` 不應該有紅色錯誤
   - 所有 Spring 的導入都不應該有錯誤

3. **編譯**：
   - **Build** → **Rebuild Project**
   - 應該沒有編譯錯誤

## 如果問題仍然存在

### 檢查 Java SDK

1. **File** → **Project Structure** → **Project**
2. **Project SDK**: 應該是 **17**
3. **Project language level**: 應該是 **17**

### 檢查編譯器設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler** → **Java Compiler**
2. **Project bytecode version**: 應該是 **17**
3. **Per-module bytecode version**: 確認 `spring-boot-stomp` 為 **17**

### 檢查 Maven Runner

1. **File** → **Settings** → **Build Tools** → **Maven** → **Runner**
2. **JRE**: 應該是 **17** 或 **Use project JDK**

## 關鍵提示

- **Maven 命令行可以正常編譯**，說明依賴已下載
- **問題是 IntelliJ IDEA 沒有載入這些依賴到模組配置**
- **重新載入 Maven 專案是關鍵步驟**

## 快速檢查清單

- [ ] 右鍵 `pom.xml` → Maven → Reload Project
- [ ] 檢查 Maven Import 設置（自動導入已啟用）
- [ ] 檢查 Project Structure → Modules → Dependencies（應該有依賴）
- [ ] 使緩存失效並重啟（如果還是不行）
- [ ] 檢查 Java SDK 設置為 17

