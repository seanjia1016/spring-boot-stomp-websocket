package com.hejz.springbootstomp.config;

import com.hejz.springbootstomp.RedisMessageListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
// Redis 5.0 不需要 RedisURI，直接使用 RedisStandaloneConfiguration
// import io.lettuce.core.RedisURI;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;

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
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;
    
    // Redis 5.0 不需要用戶名，只使用密碼認證
    // @Value("${spring.redis.username:}")
    // private String redisUsername;

    @Value("${spring.redis.database:0}")
    private int redisDatabase;

    @Value("${spring.redis.timeout:5000}")
    private int redisTimeout;

    @Value("${spring.redis.lettuce.pool.max-active:8}")
    private int poolMaxActive;

    @Value("${spring.redis.lettuce.pool.max-idle:8}")
    private int poolMaxIdle;

    @Value("${spring.redis.lettuce.pool.min-idle:0}")
    private int poolMinIdle;

    @Value("${spring.redis.lettuce.pool.max-wait:-1}")
    private long poolMaxWait;

    /**
     * 配置 Redis 連接工廠
     * 
     * <p>使用 Spring Boot 自動配置，只進行必要的自定義配置。
     * 根據參考資料，Lettuce 5.3.7 修復了密碼處理邏輯，應該能正確處理密碼認證。
     * 
     * <p>配置說明：
     * <ul>
     *   <li>使用 Spring Boot 自動配置的 RedisConnectionFactory</li>
     *   <li>只保留必要的自定義配置（連接池等）</li>
     *   <li>密碼通過 application.properties 配置，由 Spring Boot 自動處理</li>
     * </ul>
     * 
     * @return 配置完成的 RedisConnectionFactory 實例
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        // 使用 Spring Boot 自動配置，只進行必要的自定義
        // 確保使用127.0.0.1而不是localhost，避免DNS解析問題
        String actualHost = redisHost;
        if (redisHost == null || redisHost.trim().isEmpty() || 
            "localhost".equalsIgnoreCase(redisHost.trim())) {
            actualHost = "127.0.0.1";
        }
        
        // 強制解析為IP地址，避免DNS解析問題
        String resolvedHost = actualHost;
        try {
            InetAddress inetAddress = InetAddress.getByName(actualHost);
            resolvedHost = inetAddress.getHostAddress();
            log.info("Redis 主機解析：{} -> {}", actualHost, resolvedHost);
        } catch (Exception e) {
            log.warn("無法解析主機名 {}，使用原始值: {}", actualHost, e.getMessage());
        }
        
        // 使用 RedisStandaloneConfiguration（Redis 5.0 只使用密碼，不需要用戶名）
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(resolvedHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        
        // Redis 5.0 只使用密碼認證，不需要用戶名
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword.trim()));
            log.info("Redis 密碼已設置（Redis 5.0 只使用密碼認證）");
        } else {
            log.warn("Redis 未配置密碼（使用無密碼連接）");
        }
        
        log.info("Redis 配置：host={}, port={}, database={}", 
                redisConfig.getHostName(), redisConfig.getPort(), redisConfig.getDatabase());

        // 配置連接池
        GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(poolMaxActive);
        poolConfig.setMaxIdle(poolMaxIdle);
        poolConfig.setMinIdle(poolMinIdle);
        poolConfig.setMaxWaitMillis(poolMaxWait);

        // 配置 Lettuce 客戶端
        // 根據參考資料，強制使用 RESP2 協議可以解決兼容性問題
        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .protocolVersion(ProtocolVersion.RESP2)  // 強制使用 RESP2 協議
                .build();
        
        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .clientOptions(clientOptions)
                .build();

        // 創建連接工廠（讓 Spring Boot 自動配置處理密碼）
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        // 確保使用解析後的IP地址，避免 Lettuce 內部再次解析
        factory.setHostName(resolvedHost);
        factory.setPort(redisPort);
        
        try {
            factory.afterPropertiesSet();
            log.info("Redis 連接工廠已配置（使用 Spring Boot 自動配置）：host={}, port={}, database={}, poolMaxActive={}, poolMaxIdle={}, poolMinIdle={}", 
                    resolvedHost, redisPort, redisDatabase, poolMaxActive, poolMaxIdle, poolMinIdle);
        } catch (Exception e) {
            log.error("Redis 連接工廠初始化失敗：host={}, port={}, password={}", 
                    resolvedHost, redisPort, redisPassword != null && !redisPassword.isEmpty() ? "***" : "未設置", e);
            throw new RuntimeException("Redis 連接工廠初始化失敗", e);
        }
        
        return factory;
    }

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
    public RedisTemplate<String, Object> redisTemplate(@Qualifier("redisConnectionFactory") RedisConnectionFactory connectionFactory) {
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
     * 配置 Redis 訊息監聽器容器的專用連接工廠
     * 
     * <p>RedisMessageListenerContainer 需要一個非池化的連接工廠，因為它需要阻塞連接。
     * 此方法創建一個專用的連接工廠，不使用連接池。
     * 
     * @return 配置完成的 RedisConnectionFactory 實例（非池化）
     */
    @Bean(name = "subscriptionConnectionFactory")
    public RedisConnectionFactory subscriptionConnectionFactory() {
        // 配置 Redis 連接資訊
        // 確保使用127.0.0.1而不是localhost，避免DNS解析問題
        // 強制使用IP地址，避免DNS解析導致的<unresolved>錯誤
        String actualHost = redisHost;
        if (redisHost == null || redisHost.trim().isEmpty() || 
            "localhost".equalsIgnoreCase(redisHost.trim())) {
            actualHost = "127.0.0.1";
        }
        // 強制解析為IP地址，避免DNS解析問題
        String resolvedHost = actualHost;
        try {
            InetAddress inetAddress = InetAddress.getByName(actualHost);
            resolvedHost = inetAddress.getHostAddress();
            log.info("Redis 訂閱連接主機解析：{} -> {}", actualHost, resolvedHost);
        } catch (Exception e) {
            log.warn("無法解析主機名 {}，使用原始值: {}", actualHost, e.getMessage());
        }
        // 使用 RedisStandaloneConfiguration（Redis 5.0 只使用密碼，不需要用戶名）
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(resolvedHost);
        redisConfig.setPort(redisPort);
        redisConfig.setDatabase(redisDatabase);
        
        // Redis 5.0 只使用密碼認證，不需要用戶名
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword.trim()));
            log.info("Redis 訂閱連接密碼已設置（Redis 5.0 只使用密碼認證）");
        } else {
            log.warn("Redis 訂閱連接未配置密碼（使用無密碼連接）");
        }
        
        log.info("Redis 訂閱連接配置：host={}, port={}, database={}", 
                redisConfig.getHostName(), redisConfig.getPort(), redisConfig.getDatabase());

        // 配置 Lettuce 客戶端（不使用連接池，因為 RedisMessageListenerContainer 需要阻塞連接）
        // 根據參考資料，強制使用 RESP2 協議可以解決兼容性問題
        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .protocolVersion(ProtocolVersion.RESP2)  // 強制使用 RESP2 協議
                .build();
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redisTimeout))
                .clientOptions(clientOptions)
                .build();

        // 創建連接工廠（非池化，讓 Spring Boot 自動配置處理密碼）
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
        // 確保使用解析後的IP地址，避免 Lettuce 內部再次解析
        factory.setHostName(resolvedHost);
        factory.setPort(redisPort);
        factory.afterPropertiesSet();
        
        log.info("RedisMessageListenerContainer 專用連接工廠已配置：host={}, port={}, database={}（非池化）", 
                resolvedHost, redisPort, redisDatabase);
        
        return factory;
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
     *   <li>連接工廠：使用專用的非池化連接工廠</li>
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
     * @param subscriptionConnectionFactory Redis 連接工廠（專用，非池化）
     * @param redisMessageListener Redis 訊息監聽器
     * @return 配置完成的 RedisMessageListenerContainer 實例
     * 
     * @see com.hejz.springbootstomp.RedisConfigTests#testRedisMessageListenerContainer()
     * @see com.hejz.springbootstomp.RedisConfigTests#testRedisMessageListenerContainerSubscribesToChannel()
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            @Qualifier("subscriptionConnectionFactory") RedisConnectionFactory subscriptionConnectionFactory,
            RedisMessageListener redisMessageListener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(subscriptionConnectionFactory);
        
        // 設置訂閱執行器（必須設置，否則啟動時會出現 NullPointerException）
        Executor executor = Executors.newFixedThreadPool(2);
        container.setSubscriptionExecutor(executor);
        
        // 設置連接執行器（用於處理連接相關任務）
        container.setTaskExecutor(executor);
        
        // 設置最大訂閱連接數等待時間（毫秒）
        container.setMaxSubscriptionRegistrationWaitingTime(5000);
        
        // 創建訊息監聽器適配器，綁定到 RedisMessageListener.onMessage() 方法
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(redisMessageListener, "onMessage");
        // 訂閱 /topic/chat 頻道（公共訊息）
        container.addMessageListener(listenerAdapter, new ChannelTopic("/topic/chat"));
        // 訂閱 /topic/privateMessage 頻道（私信訊息）
        container.addMessageListener(listenerAdapter, new ChannelTopic("/topic/privateMessage"));
        log.info("已訂閱 Redis 頻道: /topic/chat, /topic/privateMessage");
        
        // 設置錯誤處理：當連接失敗時，降低日誌級別，避免大量錯誤日誌
        // 注意：RedisMessageListenerContainer 會自動重試連接，這是正常行為
        // 如果 Redis 暫時不可用，容器會持續重試，直到連接成功
        
        log.info("RedisMessageListenerContainer 已配置，將在應用程式啟動後自動連接 Redis");
        
        // 不在此處手動啟動容器，讓 Spring 自動管理生命週期
        // Spring 會在應用程式啟動完成後自動啟動容器
        // 這樣可以確保在 Redis 連接準備好後再啟動，避免連接失敗
        
        return container;
    }
}

