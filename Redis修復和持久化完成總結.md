# Redis 修復和持久化功能完成總結

## ✅ 已完成的工作

### 1. Redis 連接修復

**問題：**
- `RedisTemplate` 無法連接到 Redis
- 錯誤：`Unable to connect to localhost/<unresolved>:6379`
- 錯誤：`Could not get a resource from the pool`

**解決方案：**
- ✅ 手動配置 `RedisConnectionFactory`（使用連接池）
- ✅ 為 `RedisMessageListenerContainer` 創建專用連接工廠（非池化）
- ✅ 確保連接池正確初始化

**修改文件：**
- `src/main/java/com/hejz/springbootstomp/config/RedisConfig.java`

### 2. Redis 持久化功能（使用 Lua 腳本）

**實作內容：**
- ✅ 創建 `save_message.lua` - 保存訊息的 Lua 腳本（原子性操作）
- ✅ 創建 `get_messages.lua` - 獲取訊息的 Lua 腳本（高效查詢）
- ✅ 創建 `ChatMessage` DTO - 訊息資料結構
- ✅ 創建 `ChatMessageService` - 訊息持久化服務
- ✅ 創建 `ChatHistoryController` - 歷史記錄 REST API
- ✅ 修改 `MessageController` - 添加訊息持久化調用

**存儲結構：**
- 公共訊息：`chat:messages:public`（Redis Sorted Set）
- 私信訊息：`chat:messages:private:{userId}`（Redis Sorted Set）

**功能特點：**
- 使用 Lua 腳本確保原子性操作
- 使用 Redis Sorted Set，以時間戳作為分數，自動排序
- 自動清理超過 1000 條的舊訊息
- 設置 30 天過期時間
- 無需資料庫，完全使用 Redis 存儲

## 📁 創建的文件

### Lua 腳本
1. `src/main/resources/lua/save_message.lua`
2. `src/main/resources/lua/get_messages.lua`

### Java 類別
1. `src/main/java/com/hejz/springbootstomp/dto/ChatMessage.java`
2. `src/main/java/com/hejz/springbootstomp/service/ChatMessageService.java`
3. `src/main/java/com/hejz/springbootstomp/controller/ChatHistoryController.java`

### 修改的文件
1. `src/main/java/com/hejz/springbootstomp/config/RedisConfig.java`
2. `src/main/java/com/hejz/springbootstomp/MessageController.java`

### 說明文件
1. `Redis連接修復說明.md`
2. `Redis持久化功能說明.md`
3. `測試指南.md`

## 🔌 API 端點

### 查詢公共訊息歷史
```
GET /api/chat/public?limit=50&offset=0
```

**參數：**
- `limit`：獲取數量（預設 50，最大 100）
- `offset`：偏移量（預設 0，從最新開始）

**回應範例：**
```json
{
  "success": true,
  "messages": [
    {
      "senderId": "user123",
      "senderName": "user123",
      "content": "訊息內容",
      "timestamp": 1234567890123,
      "type": "public",
      "recipientId": null
    }
  ],
  "count": 1,
  "limit": 50,
  "offset": 0
}
```

### 查詢私信歷史
```
GET /api/chat/private?userId={userId}&limit=50&offset=0
```

**參數：**
- `userId`：用戶 ID（必填）
- `limit`：獲取數量（預設 50，最大 100）
- `offset`：偏移量（預設 0，從最新開始）

## 🧪 測試方法

### 1. 測試公共聊天室

1. 打開 `http://localhost:8080/agent-a.html`
2. 打開 `http://localhost:8080/agent-b.html`
3. 在任一頁面發送公共訊息
4. 兩個頁面都應該收到訊息

### 2. 測試訊息持久化

```bash
# 發送幾條訊息後，查詢歷史記錄
curl http://localhost:8080/api/chat/public?limit=10
```

### 3. 驗證 Redis 存儲

```bash
# 檢查公共訊息數量
docker exec redis redis-cli ZCARD "chat:messages:public"

# 查看最新的公共訊息
docker exec redis redis-cli ZREVRANGE "chat:messages:public" 0 0
```

## 📊 資料流程

### 公共訊息流程

```
客戶端發送訊息
    ↓
MessageController.message()
    ↓
1. 發布到 Redis Pub/Sub（即時廣播）
2. 保存到 Redis Sorted Set（持久化，使用 Lua 腳本）
    ↓
RedisMessageListener 接收 Pub/Sub 訊息
    ↓
轉發到所有 WebSocket 客戶端
```

### 私信流程

```
客戶端發送私信
    ↓
MessageController.privateMessage()
    ↓
1. 直接發送給目標用戶（即時）
2. 保存到 Redis Sorted Set（持久化，使用 Lua 腳本）
   - 為發送者保存：chat:messages:private:{senderId}
   - 為接收者保存：chat:messages:private:{recipientId}
```

## 🔍 檢查清單

### 應用程式啟動後應該看到：

- ✅ `Redis 連接工廠已配置：host=localhost, port=6379...`
- ✅ `RedisMessageListenerContainer 專用連接工廠已配置...`
- ✅ `Started SpringBootStompApplication`

### 發送訊息後應該看到：

- ✅ `=== 公共訊息接收 ===`
- ✅ `公共訊息已發布到 Redis 並持久化`
- ✅ `公共訊息已保存到 Redis：senderId=xxx, count=X`
- ✅ `=== Redis 訊息接收 ===`
- ✅ `✓ 訊息已轉發到 WebSocket /topic/chat 頻道`

### 不應該看到的錯誤：

- ❌ `Unable to connect to localhost/<unresolved>:6379`
- ❌ `Could not get a resource from the pool`
- ❌ `RedisConnectionFailureException`

## 📝 注意事項

1. **RedisMessageListenerContainer 連接錯誤：**
   - 如果看到 `Connection failure occurred. Restarting subscription task after 5000 ms`
   - 這是正常的，容器會自動重試連接
   - 如果持續失敗，檢查 Redis 容器是否正常運行

2. **訊息持久化：**
   - 訊息以 JSON 字串形式存儲在 Redis Sorted Set 中
   - 使用時間戳（毫秒）作為分數，確保按時間排序
   - 超過 1000 條會自動刪除最舊的訊息

3. **私信持久化：**
   - 為發送者和接收者分別保存，確保雙方都能查看歷史記錄
   - 每個用戶的私信存儲在獨立的 key 中

## 🚀 下一步

應用程式已在背景啟動，請：

1. **等待應用程式完全啟動**（約 30-60 秒）
2. **測試公共聊天室功能**
3. **測試訊息持久化功能**
4. **查看日誌確認功能正常**

如果需要查看應用程式狀態，可以執行：
```bash
# 查看最新日誌
Get-Content logs\spring-boot-stomp*.log -Tail 50 | Select-String -Pattern "Redis|Started|ERROR"
```









