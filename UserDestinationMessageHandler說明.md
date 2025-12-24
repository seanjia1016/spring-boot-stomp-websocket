# UserDestinationMessageHandler 完整說明

## 1. UserDestinationMessageHandler 的位置

**UserDestinationMessageHandler 是 Spring Framework 內建的類別**，不在我們的專案程式碼中。

- **完整類別路徑**：`org.springframework.messaging.simp.user.UserDestinationMessageHandler`
- **所在套件**：Spring WebSocket Messaging 模組
- **Maven 依賴**：`spring-boot-starter-websocket`（已包含）

## 2. sessionId 的來源

### sessionId 不是前端給的，而是由 Spring WebSocket 框架自動生成

**生成流程：**

1. **前端連接時**（`agent-a.html` 或 `agent-b.html`）：
```javascript
// 前端程式碼（agent-a.html 第 163-164 行）
const socket = new SockJS('/our-websocket');
stompClient = Stomp.over(socket);
```

2. **SockJS 建立連接時**，Spring WebSocket 框架會自動生成一個唯一的 sessionId：
   - 從日誌可以看到：`New WebSocketServerSockJsSession[id=u2ja0uv2]`
   - sessionId 格式：通常是 8-10 個隨機字符（如：`u2ja0uv2`、`g5nly4po`）

3. **sessionId 的用途**：
   - 用於識別每個 WebSocket 連接
   - 用於路由訊息到正確的客戶端
   - 用於 UserDestinationMessageHandler 的路徑轉換

## 3. 完整的訊息流程和程式碼

### 步驟 1：前端連接（agent-a.html）

```189:204:src/main/resources/static/agent-a.html
                // 訂閱私信頻道
                const privateSubscription = stompClient.subscribe('/user/topic/privateMessage', function(message) {
                    console.log('=== 專員A收到私信 ===');
                    console.log('原始訊息:', message);
                    console.log('訊息ID:', message.headers['message-id']);
                    console.log('目的地:', message.headers.destination);
                    console.log('訊息內容:', message.body);
                    try {
                        const data = JSON.parse(message.body);
                        console.log('解析後的資料:', data);
                        addMessage('privateMessages', data.content, 'private', data.sender || '專員B');
                    } catch (e) {
                        console.error('解析訊息失敗:', e);
                        console.log('原始body:', message.body);
                    }
                });
                console.log('專員A已訂閱私信頻道: /user/topic/privateMessage');
```

**關鍵點：**
- 前端訂閱的是：`/user/topic/privateMessage`
- 這個路徑會被 UserDestinationMessageHandler 轉換

### 步驟 2：WebSocket 握手（Userhandshakehandler.java）

```81:99:src/main/java/com/hejz/springbootstomp/Userhandshakehandler.java
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes){
        // 生成唯一的使用者 ID（UUID 格式，去除連字號）
        // 在生產環境中，此 ID 可以是客戶端提供的用戶 ID 或 token 值
        final String id = UUID.randomUUID().toString().replaceAll("-","");
        log.info("登入用戶 ID: {}", id);
        
        // 將用戶 ID 寫入臨時文件，供測試腳本讀取
        try {
            Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "websocket_user_ids.json");
            String jsonLine = String.format("{\"userId\":\"%s\",\"timestamp\":%d}\n", id, System.currentTimeMillis());
            Files.write(tempFile, jsonLine.getBytes(), java.nio.file.StandardOpenOption.CREATE, 
                       java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("無法寫入用戶 ID 到臨時文件: {}", e.getMessage());
        }
        
        return new UserPrincipal(id);
    }
```

**關鍵點：**
- 這裡生成的是 **userId**（用戶ID），不是 sessionId
- userId 用於識別用戶身份（如：`576727c41b68434c9788d8788dd3d508`）
- sessionId 是由 Spring WebSocket 框架自動生成的（如：`u2ja0uv2`）

### 步驟 3：WebSocket 配置（WebSocketConfig.java）

```109:124:src/main/java/com/hejz/springbootstomp/WebSocketConfig.java
    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        // 啟用簡單代理，支援 /topic 前綴的頻道
        // /topic 用於公共頻道（如 /topic/chat）
        // 注意：/user 前綴應該由 UserDestinationMessageHandler 處理，
        //       不應該同時在 enableSimpleBroker 中啟用，這會導致路由衝突
        registry.enableSimpleBroker("/topic");
        // 設定應用程式目標前綴，客戶端發送訊息時使用（如 /ws/message）
        // 注意：必須包含前導斜線，否則路由匹配會失敗
        registry.setApplicationDestinationPrefixes("/ws");
        // 設定用戶目標前綴，用於私信功能（convertAndSendToUser 會自動加上此前綴）
        // 當使用 convertAndSendToUser(userId, "/topic/privateMessage", message) 時
        // 實際發送的路徑會是 /user/{userId}/topic/privateMessage
        // UserDestinationMessageHandler 會將此路徑轉換為實際的訂閱路徑
        registry.setUserDestinationPrefix("/user");
    }
```

**關鍵點：**
- `setUserDestinationPrefix("/user")` 啟用了 UserDestinationMessageHandler
- 這會自動註冊 `UserDestinationMessageHandler` Bean

### 步驟 4：後端發送私信（MessageController.java）

```168:175:src/main/java/com/hejz/springbootstomp/MessageController.java
        try {
            // 直接發送給目標用戶，不經過 Redis
            messagingTemplate.convertAndSendToUser(recipient, "/topic/privateMessage", responseMessage);
            log.info("私信已發送到目標用戶: {}", recipient);
        } catch (Exception e) {
            log.error("發送私信時發生錯誤: {}", e.getMessage(), e);
            throw e;
        }
```

**關鍵點：**
- `convertAndSendToUser(recipient, "/topic/privateMessage", message)` 
- 會自動轉換為：`/user/{recipient}/topic/privateMessage`
- 然後由 UserDestinationMessageHandler 處理

### 步驟 5：UserDestinationMessageHandler 轉換路徑（Spring Framework 內建）

**這是 Spring Framework 內建的類別，不在我們的專案中**

**轉換邏輯（從日誌可以看到）：**

```
前端訂閱：/user/topic/privateMessage
         ↓
UserDestinationMessageHandler 處理
         ↓
轉換為：/topic/privateMessage-user{sessionId}
```

**實際日誌範例：**
```
2025-12-19 15:53:24.680 [http-nio-8080-exec-5] TRACE o.s.w.s.m.StompSubProtocolHandler - From client: SUBSCRIBE /user/topic/privateMessage id=sub-1 session=u2ja0uv2
2025-12-19 15:53:24.683 [clientInboundChannel-9] TRACE o.s.m.s.u.UserDestinationMessageHandler - Translated /user/topic/privateMessage -> [/topic/privateMessage-useru2ja0uv2]
2025-12-19 15:53:24.684 [clientInboundChannel-9] DEBUG o.s.m.s.b.SimpleBrokerMessageHandler - Processing SUBSCRIBE destination=/topic/privateMessage-useru2ja0uv2 subscriptionId=sub-1 session=u2ja0uv2 user=576727c41b68434c9788d8788dd3d508
```

**轉換規則：**
- 輸入：`/user/topic/privateMessage`
- 輸出：`/topic/privateMessage-user{sessionId}`
- sessionId 來自：`StompHeaderAccessor.getSessionId()`

### 步驟 6：訊息攔截器記錄（WebSocketInterceptor.java）

```30:61:src/main/java/com/hejz/springbootstomp/config/WebSocketInterceptor.java
                
                // 針對出站訊息（伺服器發送給客戶端）進行特別記錄
                if (isOutboundChannel && command == StompCommand.MESSAGE) {
                    log.error("=== 出站訊息攔截（preSend）===");
                    log.error("STOMP 命令: {}", command);
                    log.error("目標路徑: {}", accessor.getDestination());
                    log.error("會話 ID: {}", accessor.getSessionId());
                    log.error("用戶: {}", accessor.getUser() != null ? accessor.getUser().getName() : "null");
                    log.error("訊息通道: {}", channelStr);
                    
                    String destination = accessor.getDestination();
                    if (destination != null && destination.contains("privateMessage")) {
                        log.error(">>> 這是私信訊息！目標: {}", destination);
                        Object payload = message.getPayload();
                        if (payload instanceof byte[]) {
                            try {
                                String payloadStr = new String((byte[]) payload, "UTF-8");
                                log.error("私信內容: {}", payloadStr);
                            } catch (Exception e) {
                                log.error("無法解析私信內容: {}", e.getMessage());
                            }
                        } else {
                            log.error("私信內容: {}", payload);
                        }
                    }
                }
                
                log.info("=== WebSocket 訊息攔截 ===");
                log.info("STOMP 命令: {}", command);
                log.info("目標路徑: {}", accessor.getDestination());
                log.info("會話 ID: {}", accessor.getSessionId());
                log.info("用戶: {}", accessor.getUser() != null ? accessor.getUser().getName() : "null");
                log.info("訊息通道: {}", channel != null ? channel.getClass().getName() : "null");
```

**關鍵點：**
- `accessor.getSessionId()` 可以獲取 sessionId
- sessionId 是從 Spring WebSocket 框架的訊息頭中獲取的

## 4. sessionId 的完整來源鏈

```
1. 前端建立連接
   ↓
2. SockJS 建立 WebSocket 連接
   ↓
3. Spring WebSocket 框架自動生成 sessionId
   - 類別：WebSocketServerSockJsSession
   - 生成時機：連接建立時
   - 格式：8-10 個隨機字符（如：u2ja0uv2）
   ↓
4. sessionId 存儲在 StompHeaderAccessor 中
   - 鍵：simpSessionId
   - 可以通過 accessor.getSessionId() 獲取
   ↓
5. UserDestinationMessageHandler 使用 sessionId 轉換路徑
   - 將 /user/topic/xxx 轉換為 /topic/xxx-user{sessionId}
```

## 5. 重要區別

| 項目 | userId | sessionId |
|------|--------|-----------|
| **生成位置** | Userhandshakehandler.java | Spring WebSocket 框架 |
| **生成時機** | WebSocket 握手時 | WebSocket 連接建立時 |
| **格式** | UUID（32 字符） | 隨機字符（8-10 字符） |
| **用途** | 識別用戶身份 | 識別 WebSocket 連接 |
| **範例** | `576727c41b68434c9788d8788dd3d508` | `u2ja0uv2` |
| **是否前端提供** | 否（後端生成） | 否（框架自動生成） |

## 6. UserDestinationMessageHandler 的原始碼位置

**UserDestinationMessageHandler 是 Spring Framework 的原始碼**，不在我們的專案中。

**查看方式：**
1. 在 IDE 中，按住 Ctrl 點擊 `UserDestinationMessageHandler` 類別名稱
2. 或者查看 Spring Framework 的原始碼：
   - GitHub：https://github.com/spring-projects/spring-framework
   - 路徑：`spring-messaging/src/main/java/org/springframework/messaging/simp/user/UserDestinationMessageHandler.java`

**核心方法：**
- `resolveDestination()`：解析用戶目標路徑
- `handleMessage()`：處理訊息並轉換路徑

## 7. 總結

1. **UserDestinationMessageHandler** 是 Spring Framework 內建的類別，不在我們的專案中
2. **sessionId** 是由 Spring WebSocket 框架自動生成的，不是前端給的
3. **sessionId** 用於識別每個 WebSocket 連接，用於路由訊息
4. **userId** 是我們自己生成的（在 Userhandshakehandler 中），用於識別用戶身份
5. **UserDestinationMessageHandler** 會將 `/user/{userId}/topic/xxx` 轉換為 `/topic/xxx-user{sessionId}`









