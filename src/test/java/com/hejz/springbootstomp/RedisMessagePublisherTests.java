package com.hejz.springbootstomp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.hejz.springbootstomp.dto.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisMessagePublisher 單元測試類別
 * 
 * <p>此測試類別驗證 Redis 訊息發布服務的功能，包括：
 * <ul>
 *   <li>ResponseMessage 物件發布</li>
 *   <li>字串訊息發布</li>
 *   <li>JSON 序列化</li>
 *   <li>Redis 發布</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testPublishWithResponseMessage() - 驗證 ResponseMessage 發布</li>
 *   <li>testPublishWithString() - 驗證字串發布</li>
 *   <li>testPublishWithNullMessage() - 驗證 null 訊息處理</li>
 *   <li>testPublishWithEmptyString() - 驗證空字串處理</li>
 *   <li>testPublishHandlesJsonException() - 驗證 JSON 異常處理</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.RedisMessagePublisher
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class RedisMessagePublisherTests {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private RedisMessagePublisher redisMessagePublisher;

    @BeforeEach
    void setUp() {
        // 手動創建實例並注入 Mock 物件
        redisMessagePublisher = new RedisMessagePublisher();
        // 使用反射設置私有欄位（實際應用中可以使用 setter 或構造函式注入）
        try {
            java.lang.reflect.Field redisTemplateField = RedisMessagePublisher.class.getDeclaredField("redisTemplate");
            redisTemplateField.setAccessible(true);
            redisTemplateField.set(redisMessagePublisher, redisTemplate);
            
            java.lang.reflect.Field objectMapperField = RedisMessagePublisher.class.getDeclaredField("objectMapper");
            objectMapperField.setAccessible(true);
            objectMapperField.set(redisMessagePublisher, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException("無法設置私有欄位", e);
        }
    }

    /**
     * 測試使用 ResponseMessage 物件發布訊息
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(ResponseMessage)
     */
    @Test
    @Order(1)
    void testPublishWithResponseMessage() throws JsonProcessingException {
        ResponseMessage message = new ResponseMessage("測試訊息");
        String jsonMessage = "{\"content\":\"測試訊息\"}";

        when(objectMapper.writeValueAsString(message)).thenReturn(jsonMessage);

        redisMessagePublisher.publish(message);

        verify(objectMapper, times(1)).writeValueAsString(message);
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試使用字串發布訊息
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(String)
     */
    @Test
    @Order(2)
    void testPublishWithString() throws JsonProcessingException {
        String message = "測試訊息";
        String jsonMessage = "{\"content\":\"測試訊息\"}";

        when(objectMapper.writeValueAsString(any(ResponseMessage.class))).thenReturn(jsonMessage);

        redisMessagePublisher.publish(message);

        verify(objectMapper, times(1)).writeValueAsString(any(ResponseMessage.class));
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試 null 訊息處理
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(ResponseMessage)
     */
    @Test
    @Order(3)
    void testPublishWithNullMessage() throws JsonProcessingException {
        ResponseMessage message = null;
        String jsonMessage = "null";

        when(objectMapper.writeValueAsString(message)).thenReturn(jsonMessage);

        redisMessagePublisher.publish(message);

        verify(objectMapper, times(1)).writeValueAsString(message);
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試空字串訊息處理
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(String)
     */
    @Test
    @Order(4)
    void testPublishWithEmptyString() throws JsonProcessingException {
        String message = "";
        String jsonMessage = "{\"content\":\"\"}";

        when(objectMapper.writeValueAsString(any(ResponseMessage.class))).thenReturn(jsonMessage);

        redisMessagePublisher.publish(message);

        verify(objectMapper, times(1)).writeValueAsString(any(ResponseMessage.class));
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試 JSON 序列化異常處理
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(ResponseMessage)
     */
    @Test
    @Order(5)
    void testPublishHandlesJsonException() throws JsonProcessingException {
        ResponseMessage message = new ResponseMessage("測試訊息");

        when(objectMapper.writeValueAsString(message)).thenThrow(new JsonProcessingException("序列化失敗") {});

        // 不應拋出異常（異常會被內部捕獲並記錄）
        try {
            redisMessagePublisher.publish(message);
        } catch (Exception e) {
            // 如果拋出異常，測試失敗
            fail("不應拋出異常: " + e.getMessage());
        }

        verify(objectMapper, times(1)).writeValueAsString(message);
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }
}

