# 強制修復 IntelliJ IDEA 依賴問題

## 問題確認

- ✅ Maven 命令行可以正常編譯（`BUILD SUCCESS`）
- ❌ IntelliJ IDEA 找不到所有依賴（lombok、Spring 等）
- ❌ `spring-boot-stomp.iml` 文件中**完全沒有 Maven 依賴引用**

## 強制解決方案

### 方案 1：刪除 .idea 並重新導入（最徹底，推薦）

⚠️ **注意**: 這會刪除所有 IntelliJ IDEA 的專案設置，但不會影響代碼

**步驟**：

1. **關閉 IntelliJ IDEA**

2. **備份 .idea 資料夾**（可選，以防需要恢復某些設置）：
   ```powershell
   Copy-Item -Path ".idea" -Destination ".idea.backup" -Recurse
   ```

3. **刪除 .idea 資料夾**

4. **刪除 spring-boot-stomp.iml 文件**

5. **重新打開 IntelliJ IDEA**

6. **File** → **Open**
   - 選擇專案根目錄（包含 `pom.xml` 的目錄）
   - 選擇 **Open as Project**

7. **如果提示選擇導入方式**：
   - 選擇 **Import project from external model** → **Maven**
   - 按照嚮導完成導入
   - 確保選擇 **Java 17** 作為 SDK

8. **等待 Maven 專案載入完成**
   - 觀察 Maven 工具視窗（右側邊欄）
   - 等待所有依賴下載完成

### 方案 2：手動觸發 Maven 導入（如果方案 1 太麻煩）

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Importing**

2. **取消勾選** "Import Maven projects automatically"

3. 點擊 **Apply** 和 **OK**

4. **再次打開設置**，**勾選回** "Import Maven projects automatically"

5. 點擊 **Apply** 和 **OK**

6. **右鍵點擊 `pom.xml`** → **Maven** → **Reload Project**

7. **等待 Maven 專案重新載入**

8. **如果還是不行**，執行方案 1

### 方案 3：檢查 Maven 自動導入設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Importing**

2. 確認以下設置：
   - ✅ **Import Maven projects automatically**（必須勾選）
   - ✅ **Automatically download**: Sources and Documentation（建議勾選）
   - ✅ **Use Maven wrapper**（如果有）

3. 點擊 **Apply** 和 **OK**

4. **右鍵點擊 `pom.xml`** → **Maven** → **Reload Project**

## 推薦操作順序

1. **先試方案 2**（手動觸發 Maven 導入）- 較快
2. **如果不行，執行方案 1**（刪除 .idea 並重新導入）- 最徹底

## 驗證修復

完成後檢查：

1. **Maven 工具視窗**（右側邊欄）：
   - 應該顯示專案的 Maven 結構
   - 所有依賴都應該可見

2. **Project Structure** → **Modules** → **Dependencies**：
   - 應該能看到所有 Maven 依賴（Spring Boot、Lombok 等）

3. **代碼編輯器**：
   - `Message.java` 中的 `import lombok.*` 不應該有紅色錯誤
   - 所有 Spring 的導入都不應該有錯誤

4. **編譯**：
   - **Build** → **Rebuild Project**
   - 應該沒有編譯錯誤

## 如果問題仍然存在

### 檢查 Maven 設置

1. **File** → **Settings** → **Build Tools** → **Maven**
2. **Maven home directory**: 應該是 `C:\Program Files\Apache\maven`
3. **User settings file**: 確認路徑正確

### 檢查 Java SDK

1. **File** → **Project Structure** → **Project**
2. **Project SDK**: 應該是 **17**
3. **Project language level**: 應該是 **17**

## 關鍵提示

- **Maven 命令行可以正常編譯**，說明依賴已下載
- **問題是 IntelliJ IDEA 沒有載入這些依賴到模組配置**
- **刪除 .idea 並重新導入是最徹底的解決方案**
