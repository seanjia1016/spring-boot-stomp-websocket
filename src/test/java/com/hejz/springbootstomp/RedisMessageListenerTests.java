package com.hejz.springbootstomp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejz.springbootstomp.dto.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisMessageListener 單元測試類別
 * 
 * <p>此測試類別驗證 Redis 訊息監聽器的功能，包括：
 * <ul>
 *   <li>訊息接收</li>
 *   <li>JSON 反序列化</li>
 *   <li>WebSocket 轉發</li>
 *   <li>異常處理</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testOnMessage() - 驗證基本訊息處理</li>
 *   <li>testOnMessageWithInvalidJson() - 驗證無效 JSON 處理</li>
 *   <li>testOnMessageForwardsToWebSocket() - 驗證 WebSocket 轉發</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.RedisMessageListener
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class RedisMessageListenerTests {

    // 模擬 WebSocket 訊息發送模板，用於將訊息轉發給 WebSocket 客戶端
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    // 模擬 JSON 解析器，用於將 Redis 收到的字串轉換成 Java 物件
    @Mock
    private ObjectMapper objectMapper;

    // 模擬 Redis 訊息物件，代表從 Redis 收到的訊息
    @Mock
    private Message redisMessage;

    // 要測試的 Redis 訊息監聽器實例
    private RedisMessageListener redisMessageListener;

    @BeforeEach
    void setUp() {
        // 建立一個新的 Redis 訊息監聽器實例
        redisMessageListener = new RedisMessageListener();
        try {
            // 使用反射機制來設定私有欄位 messagingTemplate
            // 因為這個欄位是私有的，無法直接設定，所以用反射來繞過存取限制
            java.lang.reflect.Field messagingTemplateField = RedisMessageListener.class.getDeclaredField("messagingTemplate");
            // 設定為可存取，這樣才能設定私有欄位的值
            messagingTemplateField.setAccessible(true);
            // 將模擬的 messagingTemplate 注入到監聽器中
            messagingTemplateField.set(redisMessageListener, messagingTemplate);
            
            // 使用反射機制來設定私有欄位 objectMapper
            java.lang.reflect.Field objectMapperField = RedisMessageListener.class.getDeclaredField("objectMapper");
            // 設定為可存取
            objectMapperField.setAccessible(true);
            // 將模擬的 objectMapper 注入到監聽器中
            objectMapperField.set(redisMessageListener, objectMapper);
        } catch (Exception e) {
            // 如果設定失敗，拋出執行時期異常
            throw new RuntimeException("無法設置私有欄位", e);
        }
    }

    /**
     * 測試基本訊息處理
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Redis 訊息（頻道："/topic/chat"，內容：{"content":"測試訊息"}）</li>
     *   <li>透過方法：onMessage(redisMessage, null)</li>
     *   <li>預期結果：應該將 JSON 字串解析成 ResponseMessage 物件，然後透過 WebSocket 發送到 "/topic/chat" 頻道</li>
     * </ul>
     * 
     * <p>實際場景：當其他伺服器節點透過 Redis 發布訊息時，這個監聽器會收到訊息，
     * 然後將訊息轉發給所有連接到 WebSocket 的客戶端。
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    @Test
    @Order(1)
    void testOnMessage() throws Exception {
        // 設定測試用的頻道名稱，這是 Redis 的頻道名稱，也是 WebSocket 的目標頻道
        String channel = "/topic/chat";
        // 設定測試用的訊息內容，這是 JSON 格式的字串
        String body = "{\"content\":\"測試訊息\"}";
        // 建立預期的 ResponseMessage 物件，這是 JSON 解析後應該得到的物件
        ResponseMessage responseMessage = new ResponseMessage("測試訊息");

        // 設定 Mock 行為：當呼叫 getChannel() 時，返回頻道名稱的位元組陣列
        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        // 設定 Mock 行為：當呼叫 getBody() 時，返回訊息內容的位元組陣列
        when(redisMessage.getBody()).thenReturn(body.getBytes());
        // 設定 Mock 行為：當呼叫 readValue() 解析 JSON 時，返回 ResponseMessage 物件
        when(objectMapper.readValue(body, ResponseMessage.class)).thenReturn(responseMessage);

        // 執行被測試的方法：處理從 Redis 收到的訊息
        redisMessageListener.onMessage(redisMessage, null);

        // 驗證：確認 objectMapper.readValue() 被呼叫了 1 次，用來解析 JSON
        verify(objectMapper, times(1)).readValue(body, ResponseMessage.class);
        // 驗證：確認 messagingTemplate.convertAndSend() 被呼叫了 1 次，用來發送訊息到 WebSocket
        // eq() 表示參數必須完全相等
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(responseMessage));
    }

    /**
     * 測試無效 JSON 處理
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Redis 訊息（頻道："/topic/chat"，內容："無效的 JSON"）</li>
     *   <li>透過方法：onMessage(redisMessage, null)</li>
     *   <li>預期結果：應該捕獲 JSON 解析異常，不會拋出異常，也不會發送訊息到 WebSocket</li>
     * </ul>
     * 
     * <p>實際場景：當 Redis 收到格式錯誤的 JSON 訊息時，系統應該要能處理這個錯誤，
     * 避免整個應用程式崩潰，但也不會將錯誤訊息發送給客戶端。
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    @Test
    @Order(2)
    void testOnMessageWithInvalidJson() throws Exception {
        // 設定測試用的頻道名稱
        String channel = "/topic/chat";
        // 設定測試用的無效 JSON 內容（不是有效的 JSON 格式）
        String body = "無效的 JSON";

        // 設定 Mock 行為：當呼叫 getChannel() 時，返回頻道名稱的位元組陣列
        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        // 設定 Mock 行為：當呼叫 getBody() 時，返回無效 JSON 的位元組陣列
        when(redisMessage.getBody()).thenReturn(body.getBytes());
        // 設定 Mock 行為：當呼叫 readValue() 解析無效 JSON 時，拋出異常
        when(objectMapper.readValue(body, ResponseMessage.class)).thenThrow(new RuntimeException("JSON 解析失敗"));

        // 不應拋出異常（異常會被內部捕獲）
        try {
            // 執行被測試的方法：處理從 Redis 收到的無效 JSON 訊息
            redisMessageListener.onMessage(redisMessage, null);
        } catch (Exception e) {
            // 如果拋出異常，測試失敗（因為方法內部應該要捕獲異常）
            fail("不應拋出異常: " + e.getMessage());
        }

        // 驗證：確認 objectMapper.readValue() 被呼叫了 1 次（嘗試解析 JSON）
        verify(objectMapper, times(1)).readValue(body, ResponseMessage.class);
        // 驗證：確認 messagingTemplate.convertAndSend() 沒有被呼叫（因為解析失敗，不應該發送訊息）
        // never() 表示這個方法不應該被呼叫
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/chat"), any(ResponseMessage.class));
    }

    /**
     * 測試 WebSocket 轉發
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Redis 訊息（頻道："/topic/chat"，內容：{"content":"測試訊息"}）</li>
     *   <li>透過方法：onMessage(redisMessage, null)</li>
     *   <li>預期結果：應該將訊息轉發到 WebSocket 的 "/topic/chat" 頻道，讓所有訂閱該頻道的客戶端都能收到</li>
     * </ul>
     * 
     * <p>實際場景：當 Redis 收到訊息後，監聽器會將訊息轉發給所有連接到 WebSocket 的客戶端，
     * 這樣就能實現多伺服器節點之間的訊息同步。
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    @Test
    @Order(3)
    void testOnMessageForwardsToWebSocket() throws Exception {
        // 設定測試用的頻道名稱
        String channel = "/topic/chat";
        // 設定測試用的訊息內容（JSON 格式）
        String body = "{\"content\":\"測試訊息\"}";
        // 建立預期的 ResponseMessage 物件
        ResponseMessage responseMessage = new ResponseMessage("測試訊息");

        // 設定 Mock 行為：當呼叫 getChannel() 時，返回頻道名稱的位元組陣列
        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        // 設定 Mock 行為：當呼叫 getBody() 時，返回訊息內容的位元組陣列
        when(redisMessage.getBody()).thenReturn(body.getBytes());
        // 設定 Mock 行為：當呼叫 readValue() 解析 JSON 時，返回 ResponseMessage 物件
        when(objectMapper.readValue(body, ResponseMessage.class)).thenReturn(responseMessage);

        // 執行被測試的方法：處理從 Redis 收到的訊息
        redisMessageListener.onMessage(redisMessage, null);

        // 驗證：確認 messagingTemplate.convertAndSend() 被呼叫了 1 次
        // 這表示訊息已經被轉發到 WebSocket 頻道
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(responseMessage));
    }
}
