# Redis 持久化功能說明

## 功能概述

實作了使用 **Lua 腳本 + Redis** 的訊息持久化功能，確保聊天訊息不會因為 Redis 重啟而丟失。

## 設計架構

### 1. 存儲結構

**使用 Redis Sorted Set 存儲訊息：**
- **公共訊息**：`chat:messages:public`（Sorted Set）
- **私信訊息**：`chat:messages:private:{userId}`（Sorted Set）

**為什麼使用 Sorted Set？**
- 以時間戳作為分數（score），自動按時間排序
- 支援高效範圍查詢
- 支援自動清理舊訊息

### 2. Lua 腳本

#### `save_message.lua` - 保存訊息

**功能：**
- 原子性地將訊息添加到 Sorted Set
- 自動清理超過最大數量的舊訊息
- 設置過期時間（30天）

**參數：**
- `KEYS[1]`: 訊息列表的 key
- `ARGV[1]`: 訊息 JSON 字串
- `ARGV[2]`: 時間戳（毫秒）
- `ARGV[3]`: 最大訊息數量

**返回值：** 保存後的訊息總數

#### `get_messages.lua` - 獲取訊息

**功能：**
- 原子性地獲取指定範圍的訊息
- 支援正序或反序查詢

**參數：**
- `KEYS[1]`: 訊息列表的 key
- `ARGV[1]`: 開始索引
- `ARGV[2]`: 結束索引
- `ARGV[3]`: 是否反轉順序（1=反轉，0=不反轉）

**返回值：** 訊息陣列

### 3. 核心服務

#### `ChatMessageService` - 訊息持久化服務

**主要方法：**

1. **`savePublicMessage(senderId, senderName, content)`**
   - 保存公共訊息到 Redis
   - 使用 Lua 腳本確保原子性

2. **`savePrivateMessage(senderId, senderName, recipientId, content)`**
   - 保存私信訊息到 Redis
   - 為發送者和接收者分別保存（確保雙方都能查看歷史記錄）

3. **`getPublicMessages(limit, offset)`**
   - 獲取公共訊息歷史記錄
   - 支援分頁查詢

4. **`getPrivateMessages(userId, limit, offset)`**
   - 獲取私信歷史記錄
   - 支援分頁查詢

### 4. 整合點

#### `MessageController` - 訊息控制器

**修改內容：**
- `message()` 方法：在發布訊息後，調用 `chatMessageService.savePublicMessage()` 保存訊息
- `privateMessage()` 方法：在發送私信後，調用 `chatMessageService.savePrivateMessage()` 保存訊息

#### `ChatHistoryController` - 歷史記錄 API

**新增 REST API：**

1. **`GET /api/chat/public`**
   - 查詢公共訊息歷史記錄
   - 參數：`limit`（預設 50）、`offset`（預設 0）

2. **`GET /api/chat/private?userId={userId}`**
   - 查詢私信歷史記錄
   - 參數：`userId`（必填）、`limit`（預設 50）、`offset`（預設 0）

## 訊息流程

### 公共訊息流程

```
1. 客戶端發送訊息
   ↓
2. MessageController.message() 接收
   ↓
3. 發布到 Redis Pub/Sub（用於即時廣播）
   ↓
4. 調用 chatMessageService.savePublicMessage()（持久化）
   ↓
5. 使用 Lua 腳本原子性地保存到 Redis Sorted Set
   ↓
6. RedisMessageListener 接收 Pub/Sub 訊息
   ↓
7. 轉發到所有 WebSocket 客戶端
```

### 私信流程

```
1. 客戶端發送私信
   ↓
2. MessageController.privateMessage() 接收
   ↓
3. 直接發送給目標用戶（即時）
   ↓
4. 調用 chatMessageService.savePrivateMessage()（持久化）
   ↓
5. 使用 Lua 腳本原子性地保存到 Redis Sorted Set
   （為發送者和接收者分別保存）
```

## 資料結構

### ChatMessage DTO

```java
{
    "senderId": "用戶ID",
    "senderName": "用戶名稱（可選）",
    "content": "訊息內容",
    "timestamp": 1234567890123,  // 毫秒時間戳
    "type": "public" 或 "private",
    "recipientId": "接收者ID（僅私信時使用）"
}
```

### Redis 存儲格式

**Key：** `chat:messages:public` 或 `chat:messages:private:{userId}`

**Value：** Sorted Set
- **Member：** ChatMessage 的 JSON 字串
- **Score：** 時間戳（毫秒）

## 配置參數

### 最大訊息數量

**預設值：** 1000 條/類型

**位置：** `ChatMessageService.MAX_MESSAGES`

**說明：** 超過此數量會自動刪除最舊的訊息

### 過期時間

**預設值：** 30 天

**位置：** `save_message.lua` 中的 `EXPIRE` 命令

**說明：** 30 天後自動刪除整個列表

### 預設查詢數量

**預設值：** 50 條

**位置：** `ChatMessageService.DEFAULT_LIMIT`

**說明：** 查詢歷史記錄時的預設數量

## API 使用範例

### 查詢公共訊息

```bash
# 獲取最新的 20 條公共訊息
GET http://localhost:8080/api/chat/public?limit=20&offset=0

# 回應
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
        },
        ...
    ],
    "count": 20,
    "limit": 20,
    "offset": 0
}
```

### 查詢私信

```bash
# 獲取用戶 user123 的最新 20 條私信
GET http://localhost:8080/api/chat/private?userId=user123&limit=20&offset=0

# 回應
{
    "success": true,
    "messages": [
        {
            "senderId": "user456",
            "senderName": "user456",
            "content": "私信內容",
            "timestamp": 1234567890123,
            "type": "private",
            "recipientId": "user123"
        },
        ...
    ],
    "count": 20,
    "limit": 20,
    "offset": 0,
    "userId": "user123"
}
```

## 優勢

1. **原子性操作：** 使用 Lua 腳本確保操作的原子性
2. **高效查詢：** Sorted Set 支援高效範圍查詢
3. **自動清理：** 自動清理舊訊息，避免 Redis 記憶體溢出
4. **自動過期：** 設置過期時間，自動清理長期未使用的資料
5. **無需資料庫：** 完全使用 Redis 存儲，無需關聯資料庫

## 注意事項

1. **訊息格式：** 訊息以 JSON 字串形式存儲在 Redis 中
2. **時間戳：** 使用毫秒級時間戳作為分數，確保排序準確
3. **私信保存：** 為發送者和接收者分別保存，確保雙方都能查看
4. **最大數量：** 超過最大數量的舊訊息會被自動刪除
5. **過期時間：** 30 天後整個列表會被自動刪除

## 未來擴展

1. **訊息搜尋：** 可以添加全文搜尋功能
2. **訊息統計：** 可以添加訊息統計功能（如每日訊息數量）
3. **訊息匯出：** 可以添加訊息匯出功能
4. **訊息備份：** 可以添加定期備份功能









