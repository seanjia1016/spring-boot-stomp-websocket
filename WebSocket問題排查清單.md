# WebSocket 問題排查清單

## 當前狀態
- ✅ 應用程式運行中
- ✅ Redis 連接正常
- ✅ RabbitMQ 連接正常
- ⚠️ WebSocket 訊息無法傳遞

## 已修正的問題

1. ✅ `scripts.js` 訂閱路徑從 `/topic/message` 改為 `/topic/chat`
2. ✅ 日誌級別從 `debug` 改為 `info`，方便查看
3. ✅ 添加 `RedisMessageListenerContainer.start()` 確保監聽器啟動

## 需要檢查的項目

### 1. 應用程式需要重新啟動

**重要**: 修改了 `RedisConfig.java`，需要重新編譯並啟動應用程式。

```powershell
# 停止當前應用程式（Ctrl+C）
# 然後重新啟動
mvn spring-boot:run
```

### 2. 檢查應用程式啟動日誌

啟動後，應該看到：
- Spring Boot 啟動成功
- Redis 連接成功
- 沒有錯誤訊息

### 3. 瀏覽器測試步驟

1. **開啟專員A頁面**: http://localhost:8080/agent-a.html
2. **開啟專員B頁面**: http://localhost:8080/agent-b.html
3. **檢查連接狀態**:
   - 按 F12 開啟開發者工具
   - Console 標籤應該顯示「已連接」
   - 應該看到用戶 ID
4. **檢查訂閱**:
   - Network 標籤 → WebSocket → Messages
   - 應該看到 `SUBSCRIBE` 訊息訂閱 `/topic/chat` 和 `/user/topic/privateMessage`

### 4. 測試公共訊息

1. 在專員A頁面發送公共訊息
2. **檢查伺服器日誌**，應該看到：
   ```
   === 公共訊息接收 ===
   訊息內容: xxx
   發布到 Redis /topic/chat 頻道
   公共訊息已發布到 Redis
   ```
3. **檢查 Redis 監聽器**，應該看到：
   ```
   === Redis 訊息接收 ===
   Redis 頻道: /topic/chat
   訊息內容: {"content":"xxx"}
   反序列化成功，轉發到 /topic/chat
   ✓ 訊息已轉發到 WebSocket /topic/chat 頻道
   ```
4. **檢查專員B頁面**，應該收到訊息

### 5. 測試私信

1. 在專員A頁面查看「我的用戶 ID」
2. 在專員B頁面輸入專員A的用戶 ID
3. 在專員B頁面發送私信
4. **檢查伺服器日誌**，應該看到：
   ```
   === 私信發送調試 ===
   發送者ID: xxx
   接收者ID: xxx
   訊息內容: xxx
   目標路徑: /user/{接收者ID}/topic/privateMessage
   私信已發送到目標用戶: xxx
   ```
5. **檢查專員A頁面**，應該收到私信

## 如果還是不通

### 檢查 1: Redis 監聽器是否啟動

執行以下命令測試：
```powershell
docker exec redis redis-cli PUBLISH "/topic/chat" '{"content":"測試訊息"}'
```

如果返回 `0`，表示沒有訂閱者，監聽器可能沒有啟動。
如果返回 `1` 或更大，表示有訂閱者，監聽器正常。

### 檢查 2: 查看應用程式日誌

查看應用程式控制台，確認：
- 沒有 Redis 連接錯誤
- `RedisMessageListenerContainer` 已啟動
- 沒有異常堆疊

### 檢查 3: 瀏覽器控制台

檢查是否有：
- WebSocket 連接錯誤
- STOMP 錯誤
- 訂閱錯誤

### 檢查 4: Network 標籤

檢查 WebSocket 連接的：
- 狀態碼（應該是 101 Switching Protocols）
- Messages 標籤中的 CONNECT 和 SUBSCRIBE 訊息
- 是否有錯誤訊息

## 下一步

1. **重新啟動應用程式**（重要！）
2. **執行測試步驟**
3. **查看伺服器日誌**
4. **如果還是不通，請提供**：
   - 伺服器日誌輸出
   - 瀏覽器控制台錯誤
   - Network 標籤的截圖



