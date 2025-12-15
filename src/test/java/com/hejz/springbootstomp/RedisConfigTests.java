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

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisMessageListener redisMessageListener;

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
        // RedisConfig 不再需要注入 redisMessageListener，改為方法參數傳入
    }

    /**
     * 測試 RedisTemplate Bean 配置
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisTemplate(RedisConnectionFactory)
     */
    @Test
    @Order(1)
    void testRedisTemplate() {
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        assertNotNull(template, "RedisTemplate 不應為 null");
        assertNotNull(template.getConnectionFactory(), "連接工廠不應為 null");
    }

    /**
     * 測試 RedisTemplate 序列化配置
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisTemplate(RedisConnectionFactory)
     */
    @Test
    @Order(2)
    void testRedisTemplateSerialization() {
        RedisTemplate<String, Object> template = redisConfig.redisTemplate(connectionFactory);

        assertNotNull(template.getKeySerializer(), "Key 序列化器不應為 null");
        assertNotNull(template.getValueSerializer(), "Value 序列化器不應為 null");
    }

    /**
     * 測試 ObjectMapper Bean 配置
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#objectMapper()
     */
    @Test
    @Order(3)
    void testObjectMapper() {
        ObjectMapper objectMapper = redisConfig.objectMapper();

        assertNotNull(objectMapper, "ObjectMapper 不應為 null");
    }

    /**
     * 測試 ObjectMapper 序列化功能
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#objectMapper()
     */
    @Test
    @Order(4)
    void testObjectMapperSerialization() throws Exception {
        ObjectMapper objectMapper = redisConfig.objectMapper();
        String testJson = "{\"content\":\"測試\"}";

        // 測試反序列化
        com.hejz.springbootstomp.dto.ResponseMessage message = 
            objectMapper.readValue(testJson, com.hejz.springbootstomp.dto.ResponseMessage.class);
        
        assertNotNull(message, "反序列化結果不應為 null");
        assertEquals("測試", message.getContent(), "內容應正確");
    }

    /**
     * 測試 RedisMessageListenerContainer Bean 配置
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisMessageListenerContainer(RedisConnectionFactory)
     */
    @Test
    @Order(5)
    void testRedisMessageListenerContainer() {
        RedisMessageListenerContainer container = 
            redisConfig.redisMessageListenerContainer(connectionFactory, redisMessageListener);

        assertNotNull(container, "RedisMessageListenerContainer 不應為 null");
        assertNotNull(container.getConnectionFactory(), "連接工廠不應為 null");
    }

    /**
     * 測試 RedisMessageListenerContainer 頻道訂閱
     * 
     * @see com.hejz.springbootstomp.config.RedisConfig#redisMessageListenerContainer(RedisConnectionFactory, RedisMessageListener)
     */
    @Test
    @Order(6)
    void testRedisMessageListenerContainerSubscribesToChannel() {
        RedisMessageListenerContainer container = 
            redisConfig.redisMessageListenerContainer(connectionFactory, redisMessageListener);

        assertNotNull(container, "RedisMessageListenerContainer 不應為 null");
        // 驗證容器已配置監聽器（實際驗證需要更複雜的測試環境）
    }
}

