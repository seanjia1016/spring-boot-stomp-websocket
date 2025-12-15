# Maven 依賴解析錯誤解決方案

## 錯誤訊息
```
Unresolved dependency: 'org.slf4j:jul-to-slf4j:jar:1.7.32'
```

## 問題分析

Maven 命令行可以成功解析依賴（`mvn dependency:resolve` 顯示 BUILD SUCCESS），但 IntelliJ IDEA 顯示未解析。這通常是 IntelliJ IDEA 的 Maven 索引或緩存問題。

## 解決方案

### 方案 1：重新載入 Maven 專案（最簡單）

1. **右鍵點擊 `pom.xml`**
2. 選擇 **Maven** → **Reload Project**
3. 等待 Maven 專案重新載入完成

### 方案 2：更新 Maven 專案

1. 打開 **Maven** 工具視窗（右側邊欄）
2. 點擊 **🔄**（重新載入圖標）或 **⚙️**（設置圖標）→ **Reload All Maven Projects**
3. 等待更新完成

### 方案 3：使緩存失效並重啟（如果方案 1 和 2 不行）

1. **File** → **Invalidate Caches / Restart...**
2. 選擇 **Invalidate and Restart**
3. 等待 IntelliJ IDEA 完全重啟
4. 重新載入 Maven 專案

### 方案 4：手動更新 Maven 索引

1. **File** → **Settings** → **Build, Execution, Deployment** → **Build Tools** → **Maven** → **Repositories**
2. 選擇 **central** 倉庫
3. 點擊 **Update** 按鈕
4. 等待索引更新完成
5. 重新載入 Maven 專案

### 方案 5：清理並重新下載依賴

在 IntelliJ IDEA 的終端中執行：

```powershell
mvn clean install -U
```

`-U` 參數會強制更新所有依賴。

### 方案 6：檢查 Maven 本地倉庫

確認依賴文件是否存在：
- 路徑：`C:\Users\您的用戶名\.m2\repository\org\slf4j\jul-to-slf4j\1.7.32\`

如果文件不存在，執行：
```powershell
mvn dependency:resolve -U
```

## 驗證

完成以上步驟後：
1. 檢查 **Maven** 工具視窗，應該能看到所有依賴
2. 檢查代碼中的導入語句，應該沒有紅色錯誤標記
3. 嘗試編譯專案，應該不會有依賴錯誤

## 注意事項

- `org.slf4j:jul-to-slf4j:1.7.32` 是 Spring Boot 2.6.1 的傳遞依賴，不需要手動添加
- 如果問題持續存在，可能是網絡問題導致無法下載依賴
- 確保 Maven 設置中的倉庫配置正確

