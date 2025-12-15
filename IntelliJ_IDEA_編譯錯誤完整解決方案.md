# IntelliJ IDEA 編譯錯誤完整解決方案

## 當前問題

IntelliJ IDEA 顯示以下錯誤：
1. `java: Compilation failed: internal java compiler error`
2. `Maven resources compiler: Failed to copy 'ClientStatus.puml'`

## 已完成的修復

### 1. 更新 `.idea/compiler.xml`
- ✅ 添加了 `ClientStatus.puml` 到排除列表
- ✅ 配置了資源文件擴展名
- ✅ 設置了編譯器參數：`-source 1.8 -target 1.8 -encoding UTF-8`

### 2. 更新 `spring-boot-stomp.iml`
- ✅ 設置了 `LANGUAGE_LEVEL="JDK_1_8"`
- ✅ 配置了編譯輸出路徑
- ✅ 排除了 `.puml` 文件模式

### 3. 更新 `pom.xml`
- ✅ 配置了資源排除，排除 `src/main/java` 中的 `.puml` 文件

## 必須在 IntelliJ IDEA 中執行的操作

### 步驟 1：使緩存失效並重啟（最重要！）

1. **File** → **Invalidate Caches / Restart...**
2. 選擇 **Invalidate and Restart**
3. 等待 IntelliJ IDEA 完全重啟

### 步驟 2：檢查專案結構

1. 按 `Ctrl + Alt + Shift + S` 打開 **Project Structure**
2. **Project** 標籤頁：
   - **Project SDK**: 選擇 **1.8** 或 **Java 8**
   - **Project language level**: 選擇 **8 - Lambdas, type annotations etc.**
   - **Compiler output**: 設置為 `$PROJECT_DIR$/target` 或留空
3. **Modules** 標籤頁：
   - 選擇 `spring-boot-stomp` 模組
   - **Language level**: 選擇 **8**
   - **Target bytecode version**: 選擇 **1.8**
4. 點擊 **Apply** 和 **OK**

### 步驟 3：檢查編譯器設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler** → **Java Compiler**
2. 確認：
   - **Project bytecode version**: **1.8**
   - **Per-module bytecode version**: 確認 `spring-boot-stomp` 為 **1.8**
3. 點擊 **Apply** 和 **OK**

### 步驟 4：重新載入 Maven 專案

1. 右鍵點擊 `pom.xml`
2. 選擇 **Maven** → **Reload Project**
3. 等待 Maven 專案重新載入完成

### 步驟 5：清理並重新編譯

1. **Build** → **Clean Project**
2. 等待清理完成
3. **Build** → **Rebuild Project**
4. 檢查是否還有錯誤

## 如果問題仍然存在

### 檢查 Java SDK 配置

1. **File** → **Project Structure** → **SDKs**
2. 確認 Java 8 SDK 已正確配置
3. 如果沒有，點擊 **+** 添加 Java 8 SDK：
   - 路徑應該是：`C:\Program Files\Zulu\zulu-8`

### 檢查 Maven 設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven**
2. **Maven home directory**: 選擇系統的 Maven（`C:\Program Files\Apache\maven`）
3. **Runner** 標籤頁：
   - **JRE**: 選擇 **1.8** 或 **Use project JDK**
   - **VM options**: `-Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8`

### 手動刪除編譯輸出

如果以上方法都不行，可以手動刪除：
1. 關閉 IntelliJ IDEA
2. 刪除 `target` 目錄
3. 刪除 `.idea` 目錄（備份重要設置）
4. 重新打開 IntelliJ IDEA
5. 重新導入專案

## 驗證

完成以上步驟後：
1. 檢查 **Build** 輸出面板，應該沒有錯誤
2. 嘗試運行一個測試，確認編譯正常
3. 檢查 **Problems** 面板，確認沒有編譯錯誤

## 注意事項

- **必須執行步驟 1（使緩存失效）**，這是解決 IntelliJ IDEA 內部編譯器錯誤的關鍵
- 確保所有設置都使用 Java 8，不要混用不同版本的 Java
- 如果修改了配置，記得點擊 **Apply** 和 **OK** 保存

