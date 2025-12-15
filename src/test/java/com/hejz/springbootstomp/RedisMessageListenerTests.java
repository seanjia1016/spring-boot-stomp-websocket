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

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Message redisMessage;

    private RedisMessageListener redisMessageListener;

    @BeforeEach
    void setUp() {
        // 手動創建實例並注入 Mock 物件
        redisMessageListener = new RedisMessageListener();
        try {
            java.lang.reflect.Field messagingTemplateField = RedisMessageListener.class.getDeclaredField("messagingTemplate");
            messagingTemplateField.setAccessible(true);
            messagingTemplateField.set(redisMessageListener, messagingTemplate);
            
            java.lang.reflect.Field objectMapperField = RedisMessageListener.class.getDeclaredField("objectMapper");
            objectMapperField.setAccessible(true);
            objectMapperField.set(redisMessageListener, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException("無法設置私有欄位", e);
        }
    }

    /**
     * 測試基本訊息處理
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    @Test
    @Order(1)
    void testOnMessage() throws Exception {
        String channel = "/topic/chat";
        String body = "{\"content\":\"測試訊息\"}";
        ResponseMessage responseMessage = new ResponseMessage("測試訊息");

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(body.getBytes());
        when(objectMapper.readValue(body, ResponseMessage.class)).thenReturn(responseMessage);

        redisMessageListener.onMessage(redisMessage, null);

        verify(objectMapper, times(1)).readValue(body, ResponseMessage.class);
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(responseMessage));
    }

    /**
     * 測試無效 JSON 處理
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    @Test
    @Order(2)
    void testOnMessageWithInvalidJson() throws Exception {
        String channel = "/topic/chat";
        String body = "無效的 JSON";

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(body.getBytes());
        when(objectMapper.readValue(body, ResponseMessage.class)).thenThrow(new RuntimeException("JSON 解析失敗"));

        // 不應拋出異常（異常會被內部捕獲）
        try {
            redisMessageListener.onMessage(redisMessage, null);
        } catch (Exception e) {
            // 如果拋出異常，測試失敗
            fail("不應拋出異常: " + e.getMessage());
        }

        verify(objectMapper, times(1)).readValue(body, ResponseMessage.class);
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/chat"), any(ResponseMessage.class));
    }

    /**
     * 測試 WebSocket 轉發
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    @Test
    @Order(3)
    void testOnMessageForwardsToWebSocket() throws Exception {
        String channel = "/topic/chat";
        String body = "{\"content\":\"測試訊息\"}";
        ResponseMessage responseMessage = new ResponseMessage("測試訊息");

        when(redisMessage.getChannel()).thenReturn(channel.getBytes());
        when(redisMessage.getBody()).thenReturn(body.getBytes());
        when(objectMapper.readValue(body, ResponseMessage.class)).thenReturn(responseMessage);

        redisMessageListener.onMessage(redisMessage, null);

        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(responseMessage));
    }
}

