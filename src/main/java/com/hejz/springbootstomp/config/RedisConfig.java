package com.hejz.springbootstomp.config;

import com.hejz.springbootstomp.RedisMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Redis 配置類別
 * 
 * <p>此配置類別負責配置 Redis 相關的 Bean，包括：
 * <ul>
 *   <li>RedisTemplate：用於 Redis 資料操作和訊息發布</li>
 *   <li>ObjectMapper：用於 JSON 序列化和反序列化</li>
 *   <li>RedisMessageListenerContainer：用於監聽 Redis Pub/Sub 頻道</li>
 * </ul>
 * 
 * <p>配置說明：
 * <ul>
 *   <li>RedisTemplate 使用 String 序列化器處理 key，JSON 序列化器處理 value</li>
 *   <li>RedisMessageListenerContainer 訂閱 /topic/chat 頻道</li>
 *   <li>當收到 Redis 訊息時，會調用 RedisMessageListener.onMessage() 方法</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.RedisConfigTests
 * @see com.hejz.springbootstomp.RedisMessageListener
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Configuration
public class RedisConfig {

    /**
     * 配置 RedisTemplate Bean
     * 
     * <p>此方法創建並配置 RedisTemplate，用於 Redis 資料操作和訊息發布。
     * 配置了適當的序列化器以確保資料正確序列化和反序列化。
     * 
     * <p>序列化器配置：
     * <ul>
     *   <li>Key 序列化器：StringRedisSerializer（用於字串 key）</li>
     *   <li>Hash Key 序列化器：StringRedisSerializer（用於 Hash 的 key）</li>
     *   <li>Value 序列化器：GenericJackson2JsonRedisSerializer（用於 JSON value）</li>
     *   <li>Hash Value 序列化器：GenericJackson2JsonRedisSerializer（用於 Hash 的 JSON value）</li>
     * </ul>
     * 
     * <p>使用場景：
     * <ul>
     *   <li>Redis 資料操作：儲存、讀取、刪除資料</li>
     *   <li>訊息發布：透過 convertAndSend() 發布訊息到 Redis Pub/Sub 頻道</li>
     * </ul>
     * 
     * @param connectionFactory Redis 連接工廠，由 Spring Boot 自動配置提供
     * @return 配置完成的 RedisTemplate 實例
     * 
     * @see com.hejz.springbootstomp.RedisConfigTests#testRedisTemplate()
     * @see com.hejz.springbootstomp.RedisConfigTests#testRedisTemplateSerialization()
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用 String 序列化器作為 key 的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 使用 JSON 序列化器作為 value 的序列化器
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        // 初始化模板
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 配置 ObjectMapper Bean
     * 
     * <p>此方法創建 ObjectMapper 實例，用於 JSON 序列化和反序列化。
     * ObjectMapper 被用於將 ResponseMessage 物件轉換為 JSON 字串，以及
     * 將 JSON 字串轉換回 ResponseMessage 物件。
     * 
     * <p>使用場景：
     * <ul>
     *   <li>RedisMessagePublisher：將 ResponseMessage 序列化為 JSON</li>
     *   <li>RedisMessageListener：將 JSON 反序列化為 ResponseMessage</li>
     * </ul>
     * 
     * @return ObjectMapper 實例
     * 
     * @see com.hejz.springbootstomp.RedisConfigTests#testObjectMapper()
     * @see com.hejz.springbootstomp.RedisConfigTests#testObjectMapperSerialization()
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 配置 Redis 訊息監聽器容器
     * 
     * <p>此方法創建並配置 RedisMessageListenerContainer，用於監聽 Redis Pub/Sub 頻道。
     * 當收到訊息時，會調用 RedisMessageListener.onMessage() 方法。
     * 
     * <p>配置說明：
     * <ul>
     *   <li>訂閱頻道：/topic/chat</li>
     *   <li>監聽器方法：RedisMessageListener.onMessage()</li>
     *   <li>連接工廠：使用注入的 RedisConnectionFactory</li>
     * </ul>
     * 
     * <p>工作流程：
     * <ol>
     *   <li>RedisMessageListenerContainer 啟動並訂閱 /topic/chat 頻道</li>
     *   <li>當有訊息發布到該頻道時，Redis 會通知容器</li>
     *   <li>容器調用 MessageListenerAdapter，進而調用 RedisMessageListener.onMessage()</li>
     *   <li>RedisMessageListener 將訊息轉發給 WebSocket 客戶端</li>
     * </ol>
     * 
     * @param connectionFactory Redis 連接工廠，由 Spring Boot 自動配置提供
     * @return 配置完成的 RedisMessageListenerContainer 實例
     * 
     * @see com.hejz.springbootstomp.RedisConfigTests#testRedisMessageListenerContainer()
     * @see com.hejz.springbootstomp.RedisConfigTests#testRedisMessageListenerContainerSubscribesToChannel()
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisMessageListener redisMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        
        // 創建訊息監聽器適配器，綁定到 RedisMessageListener.onMessage() 方法
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(redisMessageListener, "onMessage");
        // 訂閱 /topic/chat 頻道
        container.addMessageListener(listenerAdapter, new ChannelTopic("/topic/chat"));
        
        return container;
    }
}

