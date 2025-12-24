# Redis 和 RabbitMQ 使用情況說明

## 總結

您的感覺是**正確的**！目前的情況是：

1. **Redis** - 用於**公共聊天室**，但**目前壞了**（連接失敗）
2. **RabbitMQ** - 用於**客戶端狀態管理**（心跳檢查），**不是用於聊天室**

---

## 1. Redis 的使用情況

### 用途：公共聊天室的多節點同步

**設計目的：**
- 當應用程式部署在多個節點時，所有節點的客戶端都能收到公共訊息
- 透過 Redis Pub/Sub 機制實現跨節點訊息同步

**使用流程：**

```
1. 客戶端發送公共訊息
   ↓
2. MessageController.message() 接收
   ↓
3. redisPublisher.publish() 發布到 Redis /topic/chat 頻道
   ↓
4. RedisMessageListener 監聽 Redis 頻道
   ↓
5. 轉發到所有節點的 WebSocket 客戶端
```

**相關程式碼：**

1. **MessageController.java** (第 87 行)
```java
// 發布到 Redis，Redis 監聽器會轉發到所有節點的 WebSocket 客戶端
redisPublisher.publish(escapedContent);
```

2. **RedisMessagePublisher.java** (第 88 行)
```java
// 發布到 Redis /topic/chat 頻道
redisTemplate.convertAndSend(topic.getTopic(), jsonMessage);
```

3. **RedisMessageListener.java** (第 108 行)
```java
// 轉發到 WebSocket 的 /topic/chat 頻道，所有訂閱的客戶端都會收到
messagingTemplate.convertAndSend("/topic/chat", responseMessage);
```

### 目前狀態：❌ 壞了

**問題：**
- `RedisTemplate` 無法連接到 Redis
- 錯誤：`Unable to connect to localhost/<unresolved>:6379`
- 錯誤：`Could not get a resource from the pool`
- 導致公共聊天室無法接收訊息

**影響：**
- 公共聊天室功能完全無法使用
- 訊息無法發布到 Redis
- 客戶端無法收到公共訊息

---

## 2. RabbitMQ 的使用情況

### 用途：客戶端狀態管理（心跳檢查）

**設計目的：**
- 監控客戶端的線上/離線狀態
- 使用 TTL + 死信佇列實現延遲檢查
- 當客戶端超時未發送心跳時，自動標記為離線

**使用流程：**

```
1. 客戶端發送心跳請求（/ws/heartbeat）
   ↓
2. HeartbeatController.handleHeartbeat() 接收
   ↓
3. ClientStatusService.recordHeartbeat() 記錄到 Redis
   ↓
4. 發送延遲訊息到 RabbitMQ（延遲 60 秒）
   ↓
5. 60 秒後，ClientStatusCheckConsumer 檢查客戶端是否超時
   ↓
6. 如果超時，更新客戶端狀態為離線
```

**相關程式碼：**

1. **HeartbeatController.java** (第 78-97 行)
```java
@MessageMapping("/heartbeat")
@SendToUser("/topic/heartbeat")
public ResponseMessage handleHeartbeat(Principal principal, HeartbeatMessage heartbeatMessage) {
    // 記錄客戶端心跳到 Redis
    Long lastHeartbeatTime = clientStatusService.recordHeartbeat(clientId);
    
    // 發送延遲訊息到 RabbitMQ，用於檢查客戶端是否超時
    sendDelayedStatusCheck(clientId, lastHeartbeatTime);
    
    return new ResponseMessage("心跳已接收，最後心跳時間：" + lastHeartbeatTime);
}
```

2. **ClientStatusCheckConsumer.java** (第 66-105 行)
```java
@RabbitListener(queues = RabbitMQConfig.CLIENT_STATUS_CHECK_QUEUE)
public void handleClientStatusCheck(
        ClientStatusCheckMessage message,
        Channel channel,
        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
    
    // 檢查客戶端是否超時
    boolean isTimeout = clientStatusService.checkClientTimeout(clientId, expectedLastHeartbeatTime);
    
    if (isTimeout) {
        log.info("客戶端已超時，狀態已更新為離線：clientId={}", clientId);
    }
}
```

### 目前狀態：✅ 正常（但前端沒有使用）

**問題：**
- 前端（agent-a.html, agent-b.html）**沒有發送心跳請求**
- 所以 RabbitMQ 的功能雖然實作了，但**實際上沒有被使用**

**影響：**
- 客戶端狀態管理功能無法運作
- 無法自動檢測客戶端離線狀態

---

## 3. 功能對照表

| 功能 | Redis | RabbitMQ | 目前狀態 |
|------|-------|----------|----------|
| **公共聊天室** | ✅ 使用 | ❌ 不使用 | ❌ 壞了（Redis 連接失敗） |
| **私信功能** | ❌ 不使用 | ❌ 不使用 | ✅ 正常（不依賴 Redis/RabbitMQ） |
| **客戶端狀態管理** | ✅ 使用（存儲心跳時間） | ✅ 使用（延遲檢查） | ⚠️ 未使用（前端沒發送心跳） |

---

## 4. 為什麼感覺沒有用到？

### Redis（公共聊天室）

**原因：**
1. Redis 連接失敗，公共聊天室無法工作
2. 即使 Redis 正常，如果只有單一節點部署，也看不出多節點同步的效果
3. 私信功能不依賴 Redis，所以私信正常但公共聊天室壞了

### RabbitMQ（客戶端狀態管理）

**原因：**
1. 前端沒有實作心跳發送功能
2. 這個功能是後端實作的，但前端沒有調用
3. 即使 RabbitMQ 正常，如果前端不發送心跳，這個功能也不會運作

---

## 5. 如何驗證使用情況？

### 驗證 Redis（公共聊天室）

**如果 Redis 正常：**
1. 發送公共訊息
2. 查看日誌，應該看到：
   - `=== 公共訊息接收 ===`
   - `發布到 Redis /topic/chat 頻道`
   - `=== Redis 訊息接收 ===`
   - `✓ 訊息已轉發到 WebSocket /topic/chat 頻道`
3. 所有連接的客戶端都應該收到訊息

**目前狀態：**
- ❌ 看不到 `=== Redis 訊息接收 ===` 日誌
- ❌ 客戶端無法收到公共訊息

### 驗證 RabbitMQ（客戶端狀態管理）

**如果前端有發送心跳：**
1. 前端定期發送 `/ws/heartbeat` 請求
2. 查看日誌，應該看到：
   - `收到客戶端心跳：clientId=xxx`
   - `發送延遲客戶端狀態檢查訊息`
   - `處理客戶端狀態檢查：clientId=xxx`
3. 60 秒後檢查客戶端是否超時

**目前狀態：**
- ❌ 前端沒有發送心跳請求
- ❌ 看不到任何心跳相關的日誌

---

## 6. 建議

### 對於 Redis（公共聊天室）

**需要修復：**
1. 檢查 Redis 連接配置
2. 確認 Redis 容器正常運行
3. 修復 `RedisTemplate` 連接問題
4. 修復 `RedisMessageListenerContainer` 連接問題

### 對於 RabbitMQ（客戶端狀態管理）

**可以選擇：**
1. **選項 A：實作前端心跳功能**
   - 在 `agent-a.html` 和 `agent-b.html` 中定期發送心跳
   - 使用 `setInterval` 每 30 秒發送一次 `/ws/heartbeat` 請求

2. **選項 B：移除 RabbitMQ 相關功能**
   - 如果不需要客戶端狀態管理，可以移除相關程式碼
   - 簡化專案結構

---

## 7. 總結

1. **Redis** - 用於公共聊天室，但**目前壞了**，需要修復
2. **RabbitMQ** - 用於客戶端狀態管理，但**前端沒有使用**，功能無法運作
3. **私信功能** - 不依賴 Redis 或 RabbitMQ，所以**正常運作**

**您的感覺是對的：**
- 公共聊天室應該要用 Redis，但因為連接失敗所以沒用到
- RabbitMQ 的功能雖然實作了，但前端沒有使用，所以感覺沒有用到









