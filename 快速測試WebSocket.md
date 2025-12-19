# WebSocket 快速測試指南

## 問題：WebSocket 訊息無法傳遞

## 快速測試步驟

### 步驟 1: 開啟測試頁面

在瀏覽器中開啟兩個標籤頁：
1. **專員A**: http://localhost:8080/agent-a.html
2. **專員B**: http://localhost:8080/agent-b.html

### 步驟 2: 檢查連接狀態

在每個頁面：
1. 按 F12 開啟開發者工具
2. 切換到 **Console** 標籤
3. 確認看到：
   - ✅ "已連接" 狀態
   - ✅ "我的用戶 ID: xxxxx"
   - ✅ "專員A已訂閱私信頻道: /user/topic/privateMessage" (或專員B)

### 步驟 3: 檢查 Network 標籤

1. 切換到 **Network** 標籤
2. 選擇 **WebSocket** 過濾器
3. 點擊 **websocket** 連接
4. 切換到 **Messages** 子標籤
5. 確認看到：
   - ✅ `CONNECT` 訊息（連接成功）
   - ✅ `CONNECTED` 回應（包含 user-name）
   - ✅ `SUBSCRIBE` 訊息（訂閱 /topic/chat 和 /user/topic/privateMessage）

### 步驟 4: 測試公共訊息

1. 在專員A頁面發送公共訊息
2. 檢查：
   - 專員A頁面是否顯示訊息（自己發送的）
   - 專員B頁面是否收到訊息
   - 伺服器日誌是否有 `=== 公共訊息接收 ===`

### 步驟 5: 測試私信

1. 在專員A頁面查看「我的用戶 ID」
2. 在專員B頁面：
   - 輸入專員A的用戶 ID（或點擊「從localStorage載入」）
   - 發送私信給專員A
3. 檢查：
   - 專員A頁面是否收到私信
   - 伺服器日誌是否有 `=== 私信發送調試 ===`

## 常見問題排查

### 問題 1: 頁面顯示「未連接」

**可能原因**:
- WebSocket 連接失敗
- 應用程式未運行
- 端口被占用

**解決方法**:
1. 檢查應用程式是否運行：http://localhost:8080
2. 查看瀏覽器控制台錯誤訊息
3. 檢查 Network 標籤中 WebSocket 連接的狀態

### 問題 2: 連接成功但收不到訊息

**可能原因**:
- 訂閱路徑不正確
- Redis 監聽器未啟動
- 訊息路由問題

**檢查項目**:
1. 確認訂閱了 `/topic/chat`（公共訊息）
2. 確認訂閱了 `/user/topic/privateMessage`（私信）
3. 檢查伺服器日誌是否有錯誤
4. 檢查 Redis 是否正常運行

### 問題 3: 發送訊息但伺服器沒收到

**可能原因**:
- 發送路徑不正確
- MessageController 未正確處理

**檢查項目**:
1. 確認發送到 `/ws/message`（公共訊息）
2. 確認發送到 `/ws/privateMessage`（私信）
3. 查看伺服器日誌是否有 `=== 公共訊息接收 ===` 或 `=== 私信發送調試 ===`

### 問題 4: 伺服器收到但客戶端沒收到

**可能原因**:
- Redis Pub/Sub 問題
- WebSocket 轉發問題
- 訂閱路徑不匹配

**檢查項目**:
1. 檢查 Redis 是否正常運行
2. 查看伺服器日誌是否有 `=== Redis 訊息接收 ===`
3. 查看伺服器日誌是否有 `✓ 訊息已轉發到 WebSocket /topic/chat 頻道`
4. 確認客戶端訂閱的路徑與伺服器發送的路徑一致

## 調試命令

### 測試 Redis Pub/Sub
```powershell
# 發送測試訊息到 Redis
docker exec redis redis-cli PUBLISH "/topic/chat" '{"content":"測試訊息"}'
```

### 檢查 Redis 連接
```powershell
docker exec redis redis-cli ping
```

### 查看應用程式日誌
查看應用程式控制台輸出，應該看到：
- `=== 公共訊息接收 ===`
- `=== Redis 訊息接收 ===`
- `✓ 訊息已轉發到 WebSocket /topic/chat 頻道`
- `=== 私信發送調試 ===`

## 預期行為

### 公共訊息流程
```
專員A發送訊息
    ↓
MessageController.message() 接收
    ↓
發布到 Redis /topic/chat
    ↓
RedisMessageListener 接收
    ↓
轉發到 WebSocket /topic/chat
    ↓
專員A和專員B都收到
```

### 私信流程
```
專員B發送私信給專員A
    ↓
MessageController.privateMessage() 接收
    ↓
convertAndSendToUser(專員A的ID, "/topic/privateMessage", message)
    ↓
發送到 /user/{專員A的ID}/topic/privateMessage
    ↓
專員A收到（專員B不會收到）
```

## 如果還是不通

請提供以下資訊：
1. 瀏覽器控制台的錯誤訊息
2. 伺服器日誌的輸出
3. Network 標籤中 WebSocket 的 Messages 內容
4. 兩個頁面的用戶 ID



