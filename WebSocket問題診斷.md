# WebSocket 問題診斷報告

## 問題描述
專員A 發送訊息後，專員B 沒有收到（公共訊息和私信都沒收到）

## 已發現的問題

### 1. scripts.js 訂閱路徑錯誤
- **問題**: `scripts.js` 訂閱的是 `/topic/message`
- **正確**: 應該訂閱 `/topic/chat`
- **狀態**: ✅ 已修正

### 2. 公共訊息流程檢查
- **客戶端發送**: `/ws/message` → `MessageController.message()`
- **伺服器處理**: 發布到 Redis `/topic/chat` 頻道
- **Redis 監聽**: `RedisMessageListener` 接收並轉發到 `/topic/chat`
- **客戶端訂閱**: 
  - 專員A/B: `/topic/chat` ✅
  - scripts.js: `/topic/message` ❌ (已修正為 `/topic/chat`)

### 3. 私信流程檢查
- **客戶端發送**: `/ws/privateMessage` → `MessageController.privateMessage()`
- **伺服器處理**: `convertAndSendToUser(recipient, "/topic/privateMessage", message)`
- **實際路徑**: `/user/{recipient}/topic/privateMessage`
- **客戶端訂閱**: `/user/topic/privateMessage` (Spring 自動轉換為 `/user/{currentUserId}/topic/privateMessage`)
- **狀態**: 理論上應該匹配

## 調試步驟

### 步驟 1: 檢查連接狀態
1. 開啟專員A頁面: http://localhost:8080/agent-a.html
2. 開啟專員B頁面: http://localhost:8080/agent-b.html
3. 確認兩個頁面都顯示「已連接」
4. 檢查瀏覽器控制台（F12）是否有錯誤

### 步驟 2: 檢查用戶 ID
1. 在專員A頁面查看「我的用戶 ID」
2. 在專員B頁面查看「我的用戶 ID」
3. 確認兩個 ID 不同

### 步驟 3: 檢查訂閱
1. 在瀏覽器控制台查看是否有訂閱成功的日誌
2. 專員A應該看到: "專員A已訂閱私信頻道: /user/topic/privateMessage"
3. 專員B應該看到: "專員B已訂閱私信頻道: /user/topic/privateMessage"

### 步驟 4: 檢查伺服器日誌
1. 查看應用程式控制台輸出
2. 發送訊息時應該看到：
   ```
   === 公共訊息接收 ===
   訊息內容: xxx
   發布到 Redis /topic/chat 頻道
   公共訊息已發布到 Redis
   ```
3. 私信發送時應該看到：
   ```
   === 私信發送調試 ===
   發送者ID: xxx
   接收者ID: xxx
   訊息內容: xxx
   目標路徑: /user/{recipient}/topic/privateMessage
   私信已發送到目標用戶: xxx
   ```

### 步驟 5: 檢查 Redis
```bash
# 檢查 Redis 是否正常運行
docker exec redis redis-cli ping

# 監聽 Redis 頻道（在另一個終端執行）
docker exec redis redis-cli MONITOR
```

### 步驟 6: 檢查 Network 標籤
1. 開啟瀏覽器 DevTools → Network 標籤
2. 選擇 WebSocket 連接
3. 查看 Messages 子標籤
4. 確認：
   - CONNECT 成功
   - SUBSCRIBE 成功
   - SEND 訊息已發送
   - 是否有收到訊息（應該有 ↓ 標記的訊息）

## 可能的原因

### 1. Redis 連接問題
- **症狀**: 公共訊息無法廣播
- **檢查**: `docker exec redis redis-cli ping`
- **解決**: 確保 Redis 正常運行

### 2. 用戶 ID 不匹配
- **症狀**: 私信無法送達
- **檢查**: 確認發送時使用的 ID 與接收者的 ID 一致
- **解決**: 使用專員B頁面顯示的用戶 ID

### 3. 訂閱路徑問題
- **症狀**: 收不到任何訊息
- **檢查**: 確認訂閱路徑正確
- **解決**: 已修正 scripts.js

### 4. Spring 用戶目標路由問題
- **症狀**: 私信無法送達
- **檢查**: 查看伺服器日誌中的目標路徑
- **解決**: 確認 `setUserDestinationPrefix("/user")` 已配置

## 測試腳本

執行以下 PowerShell 腳本進行測試：
```powershell
.\WebSocket測試腳本.ps1
```

## 下一步

1. 重新編譯並啟動應用程式
2. 開啟專員A和專員B頁面
3. 發送測試訊息
4. 查看伺服器日誌和瀏覽器控制台
5. 根據日誌輸出進一步診斷問題



