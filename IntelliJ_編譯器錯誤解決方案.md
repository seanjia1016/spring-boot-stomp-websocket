# IntelliJ IDEA 編譯器錯誤解決方案

## 問題
```
java: Compilation failed: internal java compiler error
```

## 原因
IntelliJ IDEA 使用的 Java 編譯器版本與專案配置不一致，導致內部編譯器錯誤。

## 已完成的修復

### 1. 更新編譯器配置
- ✅ `.idea/compiler.xml` - 已添加 `-source 1.8 -target 1.8` 參數
- ✅ `spring-boot-stomp.iml` - 已設置 `LANGUAGE_LEVEL="JDK_1_8"`

### 2. 專案級別配置
- ✅ `.idea/misc.xml` - 已設置 `project-jdk-name="1.8"` 和 `languageLevel="JDK_1_8"`

## 需要手動檢查的設置

### 步驟 1：檢查專案結構
1. **File** → **Project Structure**（或 `Ctrl + Alt + Shift + S`）
2. 在 **Project** 標籤頁：
   - **Project SDK**: 選擇 **1.8** 或 **Java 8**
   - **Project language level**: 選擇 **8 - Lambdas, type annotations etc.**
3. 在 **Modules** 標籤頁：
   - 選擇 `spring-boot-stomp` 模組
   - **Language level**: 選擇 **8 - Lambdas, type annotations etc.**
   - **Target bytecode version**: 選擇 **1.8**

### 步驟 2：檢查編譯器設置
1. **File** → **Settings**（或 `Ctrl + Alt + S`）
2. 導航至：**Build, Execution, Deployment** → **Compiler** → **Java Compiler**
3. 確認：
   - **Project bytecode version**: **1.8**
   - **Per-module bytecode version**: 確認 `spring-boot-stomp` 模組設置為 **1.8**

### 步驟 3：使緩存失效並重新編譯
1. **File** → **Invalidate Caches / Restart...**
2. 選擇 **Invalidate and Restart**
3. 等待 IntelliJ IDEA 重啟
4. 重新編譯專案：**Build** → **Rebuild Project**

### 步驟 4：重新載入 Maven 專案
1. 右鍵點擊 `pom.xml`
2. 選擇 **Maven** → **Reload Project**

## 驗證

執行以下操作驗證修復：
1. 在 IntelliJ IDEA 中：**Build** → **Rebuild Project**
2. 如果編譯成功，應該不會再出現編譯器錯誤

## 如果問題仍然存在

1. **檢查 Java SDK 路徑**：
   - **File** → **Project Structure** → **SDKs**
   - 確認 Java 8 SDK 已正確配置
   - 如果沒有，點擊 **+** 添加 Java 8 SDK

2. **檢查環境變數**：
   - 確認系統的 `JAVA_HOME` 指向 Java 8
   - 在 IntelliJ IDEA 中：**File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Runner**
   - **JRE**: 選擇 **1.8** 或 **Use project JDK**

3. **清理並重新構建**：
   ```powershell
   mvn clean compile
   ```
   然後在 IntelliJ IDEA 中重新編譯

