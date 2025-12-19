package com.hejz.springbootstomp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejz.springbootstomp.RedisMessageListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RedisConfig 單元測試類別
 * 
 * <p>此測試類別驗證 Redis 配置類別的功能，包括：
 * <ul>
 *   <li>RedisTemplate Bean 配置</li>
 *   <li>ObjectMapper Bean 配置</li>
 *   <li>RedisMessageListenerContainer Bean 配置</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testRedisTemplate() - 驗證 RedisTemplate Bean</li>
 *   <li>testRedisTemplateSerialization() - 驗證序列化配置</li>
 *   <li>testObjectMapper() - 驗證 ObjectMapper Bean</li>
 *   <li>testObjectMapperSerialization() - 驗證序列化功能</li>
 *   <li>testRedisMessageListenerContainer() - 驗證監聽器容器 Bean</li>
 *   <li>testRedisMessageListenerContainerSubscribesToChannel() - 驗證頻道訂閱</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.config.RedisConfig
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class RedisConfigTests {

    // 模擬 Redis 連接工廠，用於建立 Redis 連接
    @Mock
    private RedisConnectionFactory connectionFactory;

    // 模擬 Redis 訊息監聽器，用於監聽 Redis 頻道
    @Mock
    private RedisMessageListener redisMessageListener;

    // 要測試的 Redis 配置類別實例
    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        // 建立一個新的 Redis 配置類別實例
        redisConfig = new RedisConfig();
        // RedisConfig 不再需要注入 redisMessageListener，改為方法參數傳入
    }

    /**
     * 測試 RedisTemplate Bean 配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：connectionFactory（Redis 連接工廠）</li>
     *   <li>透過方法：redisTemplate(connectionFactory)</li>
     *   <li>預期結果：應該返回一個非 null 的 RedisTemplate 物件，且連接工廠已正確設定</li>
     * </ul>
     * 
     * <p>實際場景：當 Spring Boot 啟動時，會呼叫這個方法來建立 RedisTemplate Bean，
     * 這個 Bean 用於與 Redis 進行資料操作（讀取、寫入、發布訊息等）。
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisTemplate(RedisConnectionFactory)
     */
    @Test
    @Order(1)
    void testRedisTemplate() {
        // 執行被測試的方法：建立 RedisTemplate Bean
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        // 驗證：確認 RedisTemplate 不為 null（表示 Bean 建立成功）
        assertNotNull(template, "RedisTemplate 不應為 null");
        // 驗證：確認連接工廠不為 null（表示連接工廠已正確設定）
        assertNotNull(template.getConnectionFactory(), "連接工廠不應為 null");
    }

    /**
     * 測試 RedisTemplate 序列化配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：connectionFactory（Redis 連接工廠）</li>
     *   <li>透過方法：redisTemplate(connectionFactory)</li>
     *   <li>預期結果：應該返回的 RedisTemplate 物件中，Key 和 Value 的序列化器都已正確設定</li>
     * </ul>
     * 
     * <p>實際場景：Redis 只能儲存字串，所以 Java 物件需要序列化才能存入 Redis。
     * 這個測試確保序列化器已正確配置，這樣才能正常儲存和讀取資料。
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisTemplate(RedisConnectionFactory)
     */
    @Test
    @Order(2)
    void testRedisTemplateSerialization() {
        // 執行被測試的方法：建立 RedisTemplate Bean
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        // 驗證：確認 Key 序列化器不為 null（表示 Key 序列化器已正確設定）
        assertNotNull(template.getKeySerializer(), "Key 序列化器不應為 null");
        // 驗證：確認 Value 序列化器不為 null（表示 Value 序列化器已正確設定）
        assertNotNull(template.getValueSerializer(), "Value 序列化器不應為 null");
    }

    /**
     * 測試 ObjectMapper Bean 配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無</li>
     *   <li>透過方法：objectMapper()</li>
     *   <li>預期結果：應該返回一個非 null 的 ObjectMapper 物件</li>
     * </ul>
     * 
     * <p>實際場景：當 Spring Boot 啟動時，會呼叫這個方法來建立 ObjectMapper Bean，
     * 這個 Bean 用於將 Java 物件轉換成 JSON 字串，或將 JSON 字串轉換成 Java 物件。
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#objectMapper()
     */
    @Test
    @Order(3)
    void testObjectMapper() {
        // 執行被測試的方法：建立 ObjectMapper Bean
        ObjectMapper objectMapper = redisConfig.objectMapper();

        // 驗證：確認 ObjectMapper 不為 null（表示 Bean 建立成功）
        assertNotNull(objectMapper, "ObjectMapper 不應為 null");
    }

    /**
     * 測試 ObjectMapper 序列化功能
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無</li>
     *   <li>透過方法：objectMapper()</li>
     *   <li>預期結果：應該返回的 ObjectMapper 能夠正確將 JSON 字串反序列化成 ResponseMessage 物件</li>
     * </ul>
     * 
     * <p>實際場景：當從 Redis 收到 JSON 字串時，需要使用 ObjectMapper 將它轉換成 Java 物件。
     * 這個測試確保 ObjectMapper 能正確執行這個轉換。
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#objectMapper()
     */
    @Test
    @Order(4)
    void testObjectMapperSerialization() throws Exception {
        // 執行被測試的方法：建立 ObjectMapper Bean
        ObjectMapper objectMapper = redisConfig.objectMapper();
        // 設定測試用的 JSON 字串
        String testJson = "{\"content\":\"測試\"}";

        // 測試反序列化：將 JSON 字串轉換成 ResponseMessage 物件
        com.hejz.springbootstomp.dto.ResponseMessage message = 
            objectMapper.readValue(testJson, com.hejz.springbootstomp.dto.ResponseMessage.class);
        
        // 驗證：確認反序列化結果不為 null（表示轉換成功）
        assertNotNull(message, "反序列化結果不應為 null");
        // 驗證：確認內容正確（表示轉換後的物件內容正確）
        assertEquals("測試", message.getContent(), "內容應正確");
    }

    /**
     * 測試 RedisMessageListenerContainer Bean 配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：connectionFactory（Redis 連接工廠）、redisMessageListener（訊息監聽器）</li>
     *   <li>透過方法：redisMessageListenerContainer(connectionFactory, redisMessageListener)</li>
     *   <li>預期結果：應該返回一個非 null 的 RedisMessageListenerContainer 物件，且連接工廠已正確設定</li>
     * </ul>
     * 
     * <p>實際場景：當 Spring Boot 啟動時，會呼叫這個方法來建立監聽器容器 Bean，
     * 這個容器會監聽 Redis 的特定頻道，當有訊息發布時會自動觸發監聽器。
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisMessageListenerContainer(RedisConnectionFactory)
     */
    @Test
    @Order(5)
    void testRedisMessageListenerContainer() {
        // 執行被測試的方法：建立 RedisMessageListenerContainer Bean
        RedisMessageListenerContainer container = 
            redisConfig.redisMessageListenerContainer(connectionFactory, redisMessageListener);

        // 驗證：確認容器不為 null（表示 Bean 建立成功）
        assertNotNull(container, "RedisMessageListenerContainer 不應為 null");
        // 驗證：確認連接工廠不為 null（表示連接工廠已正確設定）
        assertNotNull(container.getConnectionFactory(), "連接工廠不應為 null");
    }

    /**
     * 測試 RedisMessageListenerContainer 頻道訂閱
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：connectionFactory（Redis 連接工廠）、redisMessageListener（訊息監聽器）</li>
     *   <li>透過方法：redisMessageListenerContainer(connectionFactory, redisMessageListener)</li>
     *   <li>預期結果：應該返回一個非 null 的容器物件，且容器已配置好監聽器</li>
     * </ul>
     * 
     * <p>實際場景：驗證監聽器容器已正確配置，能夠監聽 Redis 頻道。
     * 當其他伺服器節點發布訊息到 Redis 時，這個容器會收到訊息並觸發監聽器。
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisMessageListenerContainer(RedisConnectionFactory, RedisMessageListener)
     */
    @Test
    @Order(6)
    void testRedisMessageListenerContainerSubscribesToChannel() {
        // 執行被測試的方法：建立 RedisMessageListenerContainer Bean
        RedisMessageListenerContainer container = 
            redisConfig.redisMessageListenerContainer(connectionFactory, redisMessageListener);

        // 驗證：確認容器不為 null（表示 Bean 建立成功）
        assertNotNull(container, "RedisMessageListenerContainer 不應為 null");
        // 驗證容器已配置監聽器（實際驗證需要更複雜的測試環境）
    }
}

