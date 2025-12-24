# Redis 連接修復說明

## 問題描述

**錯誤訊息：**
- `Unable to connect to localhost/<unresolved>:6379`
- `Could not get a resource from the pool`

**原因：**
- Spring Boot 自動配置的 `RedisConnectionFactory` 可能無法正確初始化連接池
- 連接池配置可能沒有正確應用

## 修復方案

### 修改檔案：`src/main/java/com/hejz/springbootstomp/config/RedisConfig.java`

**新增內容：**

1. **新增依賴導入：**
```java
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
```

2. **新增配置屬性注入：**
```java
@Value("${spring.redis.host:localhost}")
private String redisHost;

@Value("${spring.redis.port:6379}")
private int redisPort;

@Value("${spring.redis.password:}")
private String redisPassword;

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
```

3. **新增手動配置 RedisConnectionFactory：**
```java
@Bean
public RedisConnectionFactory redisConnectionFactory() {
    // 配置 Redis 連接資訊
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
    redisConfig.setHostName(redisHost);
    redisConfig.setPort(redisPort);
    redisConfig.setDatabase(redisDatabase);
    if (redisPassword != null && !redisPassword.isEmpty()) {
        redisConfig.setPassword(redisPassword);
    }

    // 配置連接池
    GenericObjectPoolConfig<?> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(poolMaxActive);
    poolConfig.setMaxIdle(poolMaxIdle);
    poolConfig.setMinIdle(poolMinIdle);
    poolConfig.setMaxWaitMillis(poolMaxWait);

    // 配置 Lettuce 客戶端
    LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .commandTimeout(Duration.ofMillis(redisTimeout))
            .build();

    // 創建連接工廠
    LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig, clientConfig);
    factory.afterPropertiesSet();
    
    log.info("Redis 連接工廠已配置：host={}, port={}, database={}, poolMaxActive={}, poolMaxIdle={}, poolMinIdle={}", 
            redisHost, redisPort, redisDatabase, poolMaxActive, poolMaxIdle, poolMinIdle);
    
    return factory;
}
```

## 修復原理

1. **手動配置連接工廠：**
   - 不再依賴 Spring Boot 的自動配置
   - 明確配置所有連接參數
   - 確保連接池正確初始化

2. **連接池配置：**
   - 使用 `GenericObjectPoolConfig` 配置連接池
   - 從 `application.properties` 讀取配置參數
   - 使用 `LettucePoolingClientConfiguration` 確保連接池正確應用

3. **連接資訊配置：**
   - 使用 `RedisStandaloneConfiguration` 配置 Redis 連接資訊
   - 支援密碼配置（如果有的話）
   - 設定連接超時時間

## 驗證方法

### 1. 檢查啟動日誌

**應該看到：**
```
Redis 連接工廠已配置：host=localhost, port=6379, database=0, poolMaxActive=8, poolMaxIdle=8, poolMinIdle=0
```

### 2. 測試公共聊天室

1. 打開 `http://localhost:8080/agent-a.html`
2. 打開 `http://localhost:8080/agent-b.html`
3. 在任一頁面發送公共訊息
4. 兩個頁面都應該收到訊息

### 3. 檢查日誌

**應該看到：**
```
=== 公共訊息接收 ===
訊息內容: xxx
發布到 Redis /topic/chat 頻道
=== Redis 訊息接收 ===
Redis 頻道: /topic/chat
訊息內容: {"content":"xxx"}
✓ 訊息已轉發到 WebSocket /topic/chat 頻道
```

### 4. 不應該再看到

- ❌ `Unable to connect to localhost/<unresolved>:6379`
- ❌ `Could not get a resource from the pool`
- ❌ `RedisConnectionFailureException`

## 注意事項

1. **需要重新啟動應用程式**才能生效
2. **確保 Redis 容器正在運行**（`docker ps | grep redis`）
3. **確保 Redis 端口 6379 可以連接**（`docker exec redis redis-cli ping`）

## 如果仍有問題

1. **檢查 Redis 容器狀態：**
   ```bash
   docker ps | grep redis
   docker exec redis redis-cli ping
   ```

2. **檢查應用程式日誌：**
   - 查看是否有 `Redis 連接工廠已配置` 日誌
   - 查看是否有連接錯誤

3. **檢查 application.properties：**
   - 確認 Redis 配置正確
   - 確認連接池配置正確

4. **重啟 Redis 容器：**
   ```bash
   docker restart redis
   ```









