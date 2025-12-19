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

    // 模擬 Redis 模板，用於發布訊息到 Redis
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    // 模擬 JSON 序列化器，用於將 Java 物件轉換成 JSON 字串
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // 要測試的 Redis 訊息發布器實例
    private RedisMessagePublisher redisMessagePublisher;

    @BeforeEach
    void setUp() {
        // 建立一個新的 Redis 訊息發布器實例
        redisMessagePublisher = new RedisMessagePublisher();
        // 使用反射設置私有欄位（實際應用中可以使用 setter 或構造函式注入）
        try {
            // 使用反射機制來設定私有欄位 redisTemplate
            java.lang.reflect.Field redisTemplateField = RedisMessagePublisher.class.getDeclaredField("redisTemplate");
            // 設定為可存取
            redisTemplateField.setAccessible(true);
            // 將模擬的 redisTemplate 注入到發布器中
            redisTemplateField.set(redisMessagePublisher, redisTemplate);
            
            // 使用反射機制來設定私有欄位 objectMapper
            java.lang.reflect.Field objectMapperField = RedisMessagePublisher.class.getDeclaredField("objectMapper");
            // 設定為可存取
            objectMapperField.setAccessible(true);
            // 將模擬的 objectMapper 注入到發布器中
            objectMapperField.set(redisMessagePublisher, objectMapper);
        } catch (Exception e) {
            // 如果設定失敗，拋出執行時期異常
            throw new RuntimeException("無法設置私有欄位", e);
        }
    }

    /**
     * 測試使用 ResponseMessage 物件發布訊息
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：ResponseMessage 物件（content: "測試訊息"）</li>
     *   <li>透過方法：publish(message)</li>
     *   <li>預期結果：應該將物件序列化成 JSON 字串，然後發布到 Redis 的 "/topic/chat" 頻道</li>
     * </ul>
     * 
     * <p>實際場景：當伺服器要發送訊息給所有客戶端時，會將訊息發布到 Redis，
     * 其他伺服器節點的監聽器會收到這個訊息，然後轉發給各自的 WebSocket 客戶端。
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(ResponseMessage)
     */
    @Test
    @Order(1)
    void testPublishWithResponseMessage() throws JsonProcessingException {
        // 建立測試用的 ResponseMessage 物件
        ResponseMessage message = new ResponseMessage("測試訊息");
        // 設定預期的 JSON 字串（物件序列化後應該得到的結果）
        String jsonMessage = "{\"content\":\"測試訊息\"}";

        // 設定 Mock 行為：當呼叫 writeValueAsString() 將物件轉成 JSON 時，返回預期的 JSON 字串
        when(objectMapper.writeValueAsString(message)).thenReturn(jsonMessage);

        // 執行被測試的方法：發布訊息到 Redis
        redisMessagePublisher.publish(message);

        // 驗證：確認 objectMapper.writeValueAsString() 被呼叫了 1 次，用來將物件轉成 JSON
        verify(objectMapper, times(1)).writeValueAsString(message);
        // 驗證：確認 redisTemplate.convertAndSend() 被呼叫了 1 次，用來發布訊息到 Redis
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試使用字串發布訊息
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：字串訊息（"測試訊息"）</li>
     *   <li>透過方法：publish(message)</li>
     *   <li>預期結果：應該將字串包裝成 ResponseMessage 物件，序列化成 JSON，然後發布到 Redis</li>
     * </ul>
     * 
     * <p>實際場景：當只需要發送簡單的文字訊息時，可以直接傳入字串，系統會自動包裝成 ResponseMessage 物件。
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(String)
     */
    @Test
    @Order(2)
    void testPublishWithString() throws JsonProcessingException {
        // 設定測試用的字串訊息
        String message = "測試訊息";
        // 設定預期的 JSON 字串
        String jsonMessage = "{\"content\":\"測試訊息\"}";

        // 設定 Mock 行為：當呼叫 writeValueAsString() 時，接受任何 ResponseMessage 物件，返回預期的 JSON
        // any() 表示接受任何該類型的物件
        when(objectMapper.writeValueAsString(any(ResponseMessage.class))).thenReturn(jsonMessage);

        // 執行被測試的方法：發布字串訊息到 Redis
        redisMessagePublisher.publish(message);

        // 驗證：確認 objectMapper.writeValueAsString() 被呼叫了 1 次
        // 系統會將字串包裝成 ResponseMessage 物件，然後序列化
        verify(objectMapper, times(1)).writeValueAsString(any(ResponseMessage.class));
        // 驗證：確認 redisTemplate.convertAndSend() 被呼叫了 1 次
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試 null 訊息處理
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：null 訊息</li>
     *   <li>透過方法：publish(null)</li>
     *   <li>預期結果：應該能處理 null 值，將 null 序列化成 "null" 字串，然後發布到 Redis</li>
     * </ul>
     * 
     * <p>實際場景：當傳入 null 時，系統應該要能正常處理，不會拋出異常。
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(ResponseMessage)
     */
    @Test
    @Order(3)
    void testPublishWithNullMessage() throws JsonProcessingException {
        // 設定測試用的 null 訊息
        ResponseMessage message = null;
        // 設定預期的 JSON 字串（null 序列化後會變成 "null" 字串）
        String jsonMessage = "null";

        // 設定 Mock 行為：當呼叫 writeValueAsString() 處理 null 時，返回 "null" 字串
        when(objectMapper.writeValueAsString(message)).thenReturn(jsonMessage);

        // 執行被測試的方法：發布 null 訊息到 Redis
        redisMessagePublisher.publish(message);

        // 驗證：確認 objectMapper.writeValueAsString() 被呼叫了 1 次
        verify(objectMapper, times(1)).writeValueAsString(message);
        // 驗證：確認 redisTemplate.convertAndSend() 被呼叫了 1 次
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試空字串訊息處理
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：空字串（""）</li>
     *   <li>透過方法：publish("")</li>
     *   <li>預期結果：應該將空字串包裝成 ResponseMessage 物件，序列化成 JSON，然後發布到 Redis</li>
     * </ul>
     * 
     * <p>實際場景：當傳入空字串時，系統應該要能正常處理，將空字串當作正常的訊息內容。
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(String)
     */
    @Test
    @Order(4)
    void testPublishWithEmptyString() throws JsonProcessingException {
        // 設定測試用的空字串訊息
        String message = "";
        // 設定預期的 JSON 字串（空字串包裝後應該得到的結果）
        String jsonMessage = "{\"content\":\"\"}";

        // 設定 Mock 行為：當呼叫 writeValueAsString() 時，接受任何 ResponseMessage 物件，返回預期的 JSON
        when(objectMapper.writeValueAsString(any(ResponseMessage.class))).thenReturn(jsonMessage);

        // 執行被測試的方法：發布空字串訊息到 Redis
        redisMessagePublisher.publish(message);

        // 驗證：確認 objectMapper.writeValueAsString() 被呼叫了 1 次
        verify(objectMapper, times(1)).writeValueAsString(any(ResponseMessage.class));
        // 驗證：確認 redisTemplate.convertAndSend() 被呼叫了 1 次
        verify(redisTemplate, times(1)).convertAndSend(eq("/topic/chat"), eq(jsonMessage));
    }

    /**
     * 測試 JSON 序列化異常處理
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：ResponseMessage 物件（content: "測試訊息"）</li>
     *   <li>透過方法：publish(message)</li>
     *   <li>預期結果：當序列化失敗時，應該捕獲異常，不會拋出異常，也不會發布訊息到 Redis</li>
     * </ul>
     * 
     * <p>實際場景：當物件無法序列化成 JSON 時（例如物件有循環引用），系統應該要能處理這個錯誤，
     * 避免整個應用程式崩潰，但也不會發布錯誤的訊息。
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisher#publish(ResponseMessage)
     */
    @Test
    @Order(5)
    void testPublishHandlesJsonException() throws JsonProcessingException {
        // 建立測試用的 ResponseMessage 物件
        ResponseMessage message = new ResponseMessage("測試訊息");

        // 設定 Mock 行為：當呼叫 writeValueAsString() 時，拋出 JSON 處理異常
        when(objectMapper.writeValueAsString(message)).thenThrow(new JsonProcessingException("序列化失敗") {});

        // 不應拋出異常（異常會被內部捕獲並記錄）
        try {
            // 執行被測試的方法：發布訊息到 Redis（應該會失敗，但不會拋出異常）
            redisMessagePublisher.publish(message);
        } catch (Exception e) {
            // 如果拋出異常，測試失敗（因為方法內部應該要捕獲異常）
            fail("不應拋出異常: " + e.getMessage());
        }

        // 驗證：確認 objectMapper.writeValueAsString() 被呼叫了 1 次（嘗試序列化）
        verify(objectMapper, times(1)).writeValueAsString(message);
        // 驗證：確認 redisTemplate.convertAndSend() 沒有被呼叫（因為序列化失敗，不應該發布訊息）
        // never() 表示這個方法不應該被呼叫
        verify(redisTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
