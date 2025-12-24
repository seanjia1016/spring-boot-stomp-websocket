# RabbitMQ 專員ID管理機制說明

## 概述

本機制使用 **RabbitMQ** 來處理專員ID變更通知，當專員ID被覆蓋時，會自動通知舊的連接斷開。

## 工作流程

```
專員A第一個網頁連接
    ↓
獲得ID: 3b7936c43d06412fbe40a5bd002eccb6
    ↓
存儲到Redis: agent:a:id = 3b7936c43d06412fbe40a5bd002eccb6
    ↓
專員A第二個網頁連接
    ↓
Lua腳本檢測到ID不同
    ↓
覆蓋Redis中的ID: agent:a:id = 404c0dbe69a343409ddede0e2746532f
    ↓
Lua腳本返回 status = 'replaced', oldId, newId
    ↓
AgentService.sendAgentIdChangeNotification()
    ↓
發送訊息到 RabbitMQ (agent.id.change.exchange)
    ↓
AgentIdChangeConsumer.handleAgentIdChange()
    ↓
通過 WebSocket 發送通知給舊連接 (/user/{oldId}/topic/agentIdChanged)
    ↓
前端收到通知，自動斷開連接
```

## 架構組件

### 1. RabbitMQ 配置

**文件**：`src/main/java/com/hejz/springbootstomp/config/RabbitMQConfig.java`

**新增的組件**：
- **交換器**：`agent.id.change.exchange` (DirectExchange)
- **佇列**：`agent.id.change.queue` (持久化佇列)
- **路由鍵**：`agent.id.change`

```java
@Bean
public DirectExchange agentIdChangeExchange() {
    return new DirectExchange(AGENT_ID_CHANGE_EXCHANGE);
}

@Bean
public Queue agentIdChangeQueue() {
    return QueueBuilder.durable(AGENT_ID_CHANGE_QUEUE).build();
}

@Bean
public Binding agentIdChangeBinding() {
    return BindingBuilder
            .bind(agentIdChangeQueue())
            .to(agentIdChangeExchange())
            .with(AGENT_ID_CHANGE_ROUTING_KEY);
}
```

### 2. 訊息DTO

**文件**：`src/main/java/com/hejz/springbootstomp/dto/AgentIdChangeMessage.java`

**欄位**：
- `agentType`: 專員類型（"a" 或 "b"）
- `oldId`: 舊的專員ID（被覆蓋的ID）
- `newId`: 新的專員ID（當前有效的ID）
- `agentName`: 專員名稱（"專員A" 或 "專員B"）
- `timestamp`: 變更時間戳（毫秒）

### 3. AgentService（訊息發送者）

**文件**：`src/main/java/com/hejz/springbootstomp/service/AgentService.java`

**修改的方法**：`setAgentId()`

**邏輯**：
1. 執行 Lua 腳本設置專員ID
2. 檢查返回結果中的 `status`
3. 如果 `status = "replaced"`，表示ID被覆蓋
4. 調用 `sendAgentIdChangeNotification()` 發送訊息到 RabbitMQ

```java
private void setAgentId(String key, String id, String agentName) {
    // ... 執行 Lua 腳本
    
    if (result instanceof Map) {
        Map<String, Object> resultMap = (Map<String, Object>) result;
        String status = (String) resultMap.get("status");
        
        if ("replaced".equals(status)) {
            String oldId = (String) resultMap.get("oldId");
            String newId = (String) resultMap.get("newId");
            String agentType = key.contains("agent:a") ? "a" : "b";
            
            sendAgentIdChangeNotification(agentType, oldId, newId, agentName, timestamp);
        }
    }
}
```

### 4. AgentIdChangeConsumer（訊息消費者）

**文件**：`src/main/java/com/hejz/springbootstomp/consumer/AgentIdChangeConsumer.java`

**功能**：
1. 監聽 `agent.id.change.queue` 佇列
2. 接收 `AgentIdChangeMessage` 訊息
3. 通過 WebSocket 發送通知給舊的連接

```java
@RabbitListener(queues = RabbitMQConfig.AGENT_ID_CHANGE_QUEUE)
public void handleAgentIdChange(AgentIdChangeMessage message) {
    String oldId = message.getOldId();
    String newId = message.getNewId();
    
    // 通過 WebSocket 發送通知
    String notificationContent = String.format(
            "【系統通知】您的專員ID已變更，請重新整理頁面。舊ID: %s，新ID: %s", 
            oldId, newId);
    ResponseMessage notification = new ResponseMessage(notificationContent);
    
    messagingTemplate.convertAndSendToUser(oldId, "/topic/agentIdChanged", notification);
}
```

### 5. 前端監聽

**文件**：`src/main/resources/static/agent-a.html` 和 `agent-b.html`

**功能**：
1. 訂閱 `/user/topic/agentIdChanged` 頻道
2. 收到通知後，自動斷開連接
3. 更新狀態為「已離線（ID已變更）」
4. 顯示提示訊息

```javascript
// 訂閱專員ID變更通知頻道（來自RabbitMQ）
stompClient.subscribe('/user/topic/agentIdChanged', function(message) {
    const data = JSON.parse(message.body);
    
    // 顯示通知訊息
    if (data.content) {
        addMessage('publicMessages', data.content, 'public', '系統');
    }
    
    // 自動斷開連接
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
    }
    
    // 更新狀態
    updateConnectionStatus(false);
    document.getElementById('connectionStatus').textContent = '已離線（ID已變更）';
    
    // 顯示提示
    alert('專員A的ID已變更，您的連接已斷開。請重新整理頁面。');
});
```

## 優點

### 1. 解耦
- 專員ID變更邏輯與通知邏輯分離
- 使用 RabbitMQ 作為訊息中介，提高系統可擴展性

### 2. 可靠性
- RabbitMQ 提供持久化佇列，確保訊息不丟失
- 即使消費者暫時不可用，訊息也會保留在佇列中

### 3. 即時性
- 當ID變更時，立即發送通知
- 不需要前端定期輪詢檢查

### 4. 可擴展性
- 可以輕鬆添加多個消費者處理ID變更事件
- 可以添加其他處理邏輯（如記錄日誌、發送郵件等）

## 與之前的方案對比

### 之前的方案（API輪詢）
- **方式**：前端每3秒調用API檢查ID是否有效
- **優點**：簡單直接
- **缺點**：需要前端定期輪詢，增加伺服器負載

### 現在的方案（RabbitMQ事件驅動）
- **方式**：當ID變更時，主動發送通知
- **優點**：
  - 事件驅動，更高效
  - 不需要前端輪詢
  - 更好的解耦和可擴展性
- **缺點**：需要 RabbitMQ 服務運行

## 測試方法

### 1. 啟動服務

確保以下服務正在運行：
- Redis
- RabbitMQ
- Spring Boot 應用程式

### 2. 測試步驟

1. **開啟第一個專員A網頁**
   - 訪問：`http://localhost:8080/agent-a.html`
   - 觀察ID和連接狀態
   - 確認顯示「已連線」

2. **開啟第二個專員A網頁**
   - 訪問：`http://localhost:8080/agent-a.html`
   - 觀察第一個網頁的狀態
   - 應該立即收到通知並斷開連接

3. **檢查 RabbitMQ 佇列**
   ```powershell
   # 查看佇列訊息
   docker exec rabbitmq rabbitmqctl list_queues name messages
   ```

4. **檢查日誌**
   - 查看 Spring Boot 應用程式日誌
   - 應該看到：
     - `已發送專員ID變更通知到RabbitMQ`
     - `處理專員ID變更通知`
     - `已發送ID變更通知給舊連接`

## 注意事項

1. **RabbitMQ 必須運行**
   - 如果 RabbitMQ 未運行，ID變更通知將無法發送
   - 但專員ID的設置仍然會正常工作（只是不會通知舊連接）

2. **WebSocket 連接狀態**
   - 如果舊連接已經斷開，通知將無法送達
   - 這是正常行為，因為連接已經不存在

3. **訊息格式**
   - 通知訊息使用 `ResponseMessage` DTO
   - 只包含 `content` 欄位，不包含 `sender` 欄位

## 未來擴展

1. **添加重試機制**
   - 如果通知發送失敗，可以重試
   - 使用 RabbitMQ 的延遲佇列實現

2. **添加通知歷史**
   - 將ID變更通知記錄到資料庫
   - 方便追蹤和審計

3. **多種通知方式**
   - 除了 WebSocket，還可以發送郵件、簡訊等

---

**文檔版本**：1.0  
**最後更新**：2025-12-19









