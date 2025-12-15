# Java 17 升級完成

## 已完成的配置更新

### 1. pom.xml
- ✅ `java.version`: `1.8` → `17`
- ✅ `maven-compiler-plugin`: 版本更新到 `3.11.0`
- ✅ `source` 和 `target`: `1.8` → `17`

### 2. IntelliJ IDEA 配置
- ✅ `.idea/misc.xml`: `project-jdk-name` 和 `languageLevel` → `JDK_17`
- ✅ `.idea/compiler.xml`: 編譯器選項 → `-source 17 -target 17`
- ✅ `.idea/workspace.xml`: Maven Runner JRE → `17`
- ✅ `spring-boot-stomp.iml`: `LANGUAGE_LEVEL` → `JDK_17`

### 3. Maven 配置
- ✅ `.mvn/maven.config`: 編譯器參數 → `17`
- ✅ `.mvn/jvm.config`: JVM 參數 → `17`

## 驗證結果

- ✅ Maven 編譯成功：`BUILD SUCCESS`
- ✅ 使用 Java 17 編譯：`Compiling 12 source files with javac [debug target 17]`

## 需要在 IntelliJ IDEA 中執行的操作

### 步驟 1：使緩存失效並重啟（必須）

1. **File** → **Invalidate Caches / Restart...**
2. 選擇 **Invalidate and Restart**
3. 等待 IntelliJ IDEA 完全重啟

### 步驟 2：檢查專案結構

1. 按 `Ctrl + Alt + Shift + S` 打開 **Project Structure**
2. **Project** 標籤頁：
   - **Project SDK**: 選擇 **17** 或 **Java 17**
   - **Project language level**: 選擇 **17 - Sealed types, always-strict floating-point semantics**
3. **Modules** 標籤頁：
   - 選擇 `spring-boot-stomp` 模組
   - **Language level**: 選擇 **17**
   - **Target bytecode version**: 選擇 **17**
4. 點擊 **Apply** 和 **OK**

### 步驟 3：檢查 Maven Runner 設置

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Runner**
2. 確認：
   - **JRE**: 選擇 **17** 或 **Use project JDK**
   - **VM options**: `-Dmaven.compiler.source=17 -Dmaven.compiler.target=17`
3. 點擊 **Apply** 和 **OK**

### 步驟 4：重新載入 Maven 專案

1. 右鍵點擊 `pom.xml`
2. 選擇 **Maven** → **Reload Project**
3. 等待 Maven 專案重新載入完成

### 步驟 5：重新編譯

1. **Build** → **Clean Project**
2. **Build** → **Rebuild Project**

## 注意事項

1. **Java 17 特性**: 現在可以使用 Java 17 的新特性，如：
   - Sealed classes
   - Pattern matching for switch
   - Text blocks
   - Records
   - 等等

2. **Spring Boot 2.6.1 兼容性**: Spring Boot 2.6.1 完全支持 Java 17

3. **Maven 服務器**: 使用 Java 17 後，Maven 服務器錯誤應該會消失，因為：
   - Java 17 與 Maven 3.9.9 完全兼容
   - IntelliJ IDEA 2025.2 對 Java 17 有更好的支持

## 如果遇到問題

1. **編譯錯誤**: 檢查是否所有配置都已更新為 Java 17
2. **Maven 錯誤**: 確認 Maven Runner 的 JRE 設置為 17
3. **依賴錯誤**: 重新載入 Maven 專案

## 驗證

完成所有步驟後：
1. 檢查 **Build** 輸出，應該沒有錯誤
2. 運行測試，確認所有測試通過
3. 檢查代碼中的導入語句，應該沒有錯誤

