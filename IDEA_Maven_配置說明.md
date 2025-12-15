# IntelliJ IDEA Maven Java 8 配置說明

## 已完成的配置

### 1. 項目級別配置
- ✅ `.idea/misc.xml` - 項目 JDK 已設置為 Java 8
- ✅ `.idea/workspace.xml` - MavenRunner 已配置使用 Java 8
  - `jreName`: 1.8
  - `vmOptions`: -Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8

### 2. Maven 配置
- ✅ `pom.xml` - Java 版本設置為 1.8
- ✅ `.mvn/jvm.config` - Maven JVM 參數配置
- ✅ `.mvn/maven.config` - Maven 編譯器配置

### 3. 編譯器配置
- ✅ `maven-compiler-plugin` 已配置 source 和 target 為 1.8

## 如果仍有問題，請手動檢查以下設置

### IntelliJ IDEA 設置檢查步驟：

1. **File → Settings → Build, Execution, Deployment → Build Tools → Maven**
   - Maven home directory: 確認指向正確的 Maven 安裝路徑
   - User settings file: 確認指向正確的 settings.xml

2. **File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner**
   - JRE: 應該選擇 "1.8" 或 "Use project JDK"
   - VM options: 應該包含 `-Dmaven.compiler.source=1.8 -Dmaven.compiler.target=1.8`

3. **File → Project Structure → Project**
   - Project SDK: 應該選擇 Java 8
   - Project language level: 應該選擇 8 - Lambdas, type annotations etc.

4. **重新載入 Maven 項目**
   - 右鍵點擊 `pom.xml` → `Maven` → `Reload Project`

## 驗證配置

執行以下命令驗證配置：
```bash
mvn clean compile -DskipTests
```

如果編譯成功，表示配置正確。

