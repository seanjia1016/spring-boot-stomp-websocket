# @MessageMapping 註解說明

## 什麼是 @MessageMapping？

`@MessageMapping` 是 Spring WebSocket 框架提供的註解，用於標記處理 STOMP 訊息的方法。

## 語法說明

```java
@MessageMapping("/message")
public void message(final Message message) throws InterruptedException {
    // 處理訊息的程式碼
}
```

### 各部分解釋

#### 1. `@MessageMapping("/message")`
- **作用**: 告訴 Spring 這個方法要處理發送到 `/message` 的 STOMP 訊息
- **路徑**: `/message` 是相對路徑，完整路徑是 `ws/message`（因為配置了 `setApplicationDestinationPrefixes("ws")`）
- **客戶端發送**: 客戶端使用 `stompClient.send("ws/message", {}, data)` 發送訊息時，會路由到這個方法

#### 2. `public void message(...)`
- **方法名**: `message` 可以是任何名稱，重要的是 `@MessageMapping` 註解
- **返回類型**: `void` 表示不直接返回訊息給客戶端（訊息通過其他方式發送）

#### 3. `final Message message`
- **參數類型**: `Message` 是自訂的 DTO（Data Transfer Object）類別
- **參數名**: `message` 是變數名稱，可以是任何名稱
- **`final` 關鍵字**: 表示這個參數不能被重新賦值（Java 最佳實踐）
- **自動綁定**: Spring 會自動將客戶端發送的 JSON 訊息反序列化為 `Message` 物件

## 完整範例

### 後端代碼（MessageController.java）

```java
@MessageMapping("/message")
public void message(final Message message) throws InterruptedException {
    // message 物件已經包含了客戶端發送的資料
    String content = message.getContent(); // 取得訊息內容
    
    // 處理訊息...
    redisPublisher.publish(content);
}
```

### 前端代碼（JavaScript）

```javascript
// 客戶端發送訊息
stompClient.send("ws/message", {}, JSON.stringify({
    content: "Hello, World!"
}));
```

### 訊息流程

```
客戶端發送
    ↓
stompClient.send("ws/message", {}, JSON.stringify({content: "Hello"}))
    ↓
WebSocket 連接
    ↓
Spring WebSocket 框架接收
    ↓
根據 @MessageMapping("/message") 路由到 message() 方法
    ↓
Spring 自動將 JSON 反序列化為 Message 物件
    ↓
執行 message(final Message message) 方法
    ↓
message.getContent() 取得 "Hello"
```

## Message 物件結構

```java
// Message.java
public class Message {
    private String content;  // 訊息內容
    private String id;       // 接收者 ID（用於私信）
    private String recipient; // 接收者 ID（備用）
    
    // getter 和 setter 方法...
}
```

### 客戶端發送的 JSON

```json
{
    "content": "這是訊息內容",
    "id": "接收者ID"  // 可選，用於私信
}
```

### Spring 自動轉換

Spring 會自動將上面的 JSON 轉換為 `Message` 物件：
```java
Message message = new Message();
message.setContent("這是訊息內容");
message.setId("接收者ID");
```

## 其他相關註解

### @SendTo
```java
@MessageMapping("/message")
@SendTo("/topic/chat")  // 將返回值發送到 /topic/chat
public ResponseMessage message(Message message) {
    return new ResponseMessage("回應訊息");
}
```

### @SendToUser
```java
@MessageMapping("/privateMessage")
@SendToUser("/topic/privateMessage")  // 發送給特定用戶
public ResponseMessage privateMessage(Principal principal, Message message) {
    return new ResponseMessage("私信回應");
}
```

## 參數類型說明

### 1. Message 物件
```java
public void message(final Message message)
```
- Spring 自動將 JSON 反序列化為 `Message` 物件

### 2. Principal 物件
```java
public void privateMessage(final Principal principal, final Message message)
```
- Spring 自動注入當前連接的用戶身份資訊
- `principal.getName()` 可以取得用戶 ID

### 3. 其他參數
```java
@MessageMapping("/message")
public void message(
    final Message message,           // 訊息物件
    final Principal principal,       // 用戶身份
    @Header("destination") String dest  // STOMP 標頭
) {
    // ...
}
```

## 常見問題

### Q: 為什麼方法參數要用 `final`？
A: `final` 關鍵字表示參數不能被重新賦值，這是 Java 的最佳實踐，可以：
- 防止意外修改參數
- 提高程式碼可讀性
- 幫助編譯器優化

### Q: 參數名稱重要嗎？
A: 不重要。重要的是參數類型。以下寫法都一樣：
```java
public void message(final Message message)
public void message(final Message msg)
public void message(final Message data)
```

### Q: 如何知道客戶端發送什麼資料？
A: 查看 `Message` 類別的欄位，或者查看前端發送的 JSON 結構。

## 實際應用範例

### 範例 1: 公共訊息
```java
@MessageMapping("/message")
public void message(final Message message) {
    // message.getContent() 取得客戶端發送的訊息內容
    String content = message.getContent();
    // 發布到 Redis，廣播給所有客戶端
    redisPublisher.publish(content);
}
```

### 範例 2: 私信
```java
@MessageMapping("/privateMessage")
public void privateMessage(final Principal principal, final Message message) {
    // principal.getName() 取得發送者的 ID
    String senderId = principal.getName();
    
    // message.getId() 取得接收者的 ID
    String recipientId = message.getId();
    
    // message.getContent() 取得訊息內容
    String content = message.getContent();
    
    // 發送給特定用戶
    messagingTemplate.convertAndSendToUser(recipientId, "/topic/privateMessage", content);
}
```

## 總結

- `@MessageMapping("/message")`: 標記方法處理發送到 `/message` 的 STOMP 訊息
- `final Message message`: 方法參數，Spring 自動將 JSON 轉換為 `Message` 物件
- `final`: Java 關鍵字，表示參數不可重新賦值
- `Message`: 自訂的 DTO 類別，包含訊息資料



