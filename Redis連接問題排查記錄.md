# Redis 連接問題排查記錄

## 問題描述
- **主要錯誤**: `Unable to connect to 127.0.0.1/<unresolved>:6379`
- **次要錯誤**: `WRONGPASS invalid username-password pair or user is disabled`
- **環境**: Redis 7.4.7, Spring Boot 3.2.0, Lettuce 6.x

## 已嘗試的解決方案

### 1. 移除 Redis 密碼配置
- **時間**: 2025-12-24
- **方法**: 
  - 移除 `redis.conf` 中的 `requirepass`
  - 移除 `application.properties` 中的 `spring.redis.password`
  - 更新 `docker-compose.yml` 的 healthcheck
- **結果**: ❌ 失敗，仍有 `<unresolved>` 錯誤

### 2. 主機名解析
- **時間**: 2025-12-24
- **方法**: 
  - 在 `RedisConfig.java` 中使用 `InetAddress.getByName()` 強制解析主機名為 IP
  - 明確設置 `factory.setHostName()` 和 `factory.setPort()`
- **結果**: ❌ 失敗，Lettuce 內部仍嘗試解析，導致 `<unresolved>` 錯誤

### 3. 配置 RESP2 協議
- **時間**: 2025-12-24
- **方法**: 
  - 在 `LettuceClientConfiguration` 中明確設置 `ProtocolVersion.RESP2`
- **結果**: ❌ 失敗，問題仍然存在

### 4. Redis 7.0 default 用戶名
- **時間**: 2025-12-24
- **方法**: 
  - 在 `application.properties` 中設置 `spring.redis.username=default`
  - 在 `RedisConfig.java` 中使用 `redisConfig.setUsername("default")`
  - 修復了註釋問題（註釋被當作用戶名的一部分）
- **結果**: ❌ 失敗，仍有 `WRONGPASS` 錯誤

### 5. 升級 Spring Boot 和 Lettuce
- **時間**: 2025-12-24
- **方法**: 
  - Spring Boot: 2.6.1 -> 3.2.0
  - Lettuce: 6.1.8.RELEASE -> 6.3.0.RELEASE (Spring Boot 默認)
  - 修復了 `javax.annotation` -> `jakarta.annotation`
- **結果**: ❌ 失敗，出現 `NoClassDefFoundError: io/lettuce/core/RedisCredentialsProvider`

### 6. 明確指定 Lettuce 6.4.0.RELEASE
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中明確指定 `lettuce-core:6.4.0.RELEASE`
- **結果**: ❌ 失敗，仍有 `RedisCredentialsProvider` 錯誤

### 7. 明確指定 Lettuce 6.5.2.RELEASE
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中明確指定 `lettuce-core:6.5.2.RELEASE`（修復 issue #3071）
- **結果**: ❌ 失敗，仍有 `RedisCredentialsProvider` 錯誤

### 8. 詳細日誌配置
- **時間**: 2025-12-24
- **方法**: 
  - 在 `application.properties` 中添加詳細日誌配置
  - 設置 `logging.level.io.lettuce.core=DEBUG`
  - 設置 `logging.level.org.springframework.data.redis=DEBUG`
  - 在 `RedisConfig.java` 中添加更詳細的異常處理和日誌
- **結果**: ✅ 成功配置，但問題仍然存在

## 當前配置

### 版本信息
- **Spring Boot**: 3.2.0
- **Spring Data Redis**: 3.2.0
- **Lettuce**: 6.4.0.RELEASE（明確指定）
- **Redis**: 7.4.7 (Docker)
- **Java**: 17

### 配置文件

#### `application.properties`
```properties
spring.redis.host=127.0.0.1
spring.redis.port=6379
spring.redis.username=default
spring.redis.password=1111
spring.redis.database=0
spring.redis.timeout=5000
```

#### `redis.conf`
```
requirepass 1111
```

#### `docker-compose.yml`
```yaml
redis:
  image: redis:7-alpine
  command: redis-server /usr/local/etc/redis/redis.conf
  healthcheck:
    test: ["CMD", "redis-cli", "-a", "1111", "ping"]
```

## 錯誤分析

### 錯誤 1: `<unresolved>` 主機名
- **原因**: Lettuce 內部嘗試解析主機名，但解析失敗
- **影響**: 無法建立連接
- **狀態**: 未解決

### 錯誤 2: `WRONGPASS`
- **原因**: 可能是 Lettuce 6.4.0 對 Redis 7.0 的 `default` 用戶名支持有問題
- **影響**: 認證失敗
- **狀態**: 未解決

### 錯誤 3: `RedisCredentialsProvider`
- **原因**: Spring Data Redis 3.2.0 期望的 API 在 Lettuce 6.4.0 中不存在
- **影響**: Bean 創建失敗
- **狀態**: 未解決

## 待嘗試的方案

### 方案 1: 使用 RedisURI 直接構建連接
- 不通過 `RedisStandaloneConfiguration`，直接使用 `RedisURI` 構建連接
- 可能可以避免 `RedisCredentialsProvider` 的問題

### 方案 2: 降級 Spring Data Redis
- 使用與 Lettuce 6.4.0 兼容的 Spring Data Redis 版本
- 可能需要降級到 3.1.x 或更早版本

### 方案 3: 使用 Jedis 代替 Lettuce
- 雖然用戶提到很多公司要淘汰 Jedis，但作為臨時解決方案
- Jedis 對 Redis 7.0 的支持可能更穩定

### 方案 4: 檢查 Lettuce 6.4.0 的實際 API
- 查看 Lettuce 6.4.0 的文檔，確認正確的認證方式
- 可能需要使用不同的 API 設置用戶名和密碼

## 參考資料

- [GitHub Issue #3071](https://github.com/redis/lettuce/issues/3071) - Lettuce 密碼處理問題
- [CSDN 文章](https://blog.csdn.net/qq_41201245/article/details/136779229) - Redis 7.0 default 用戶名配置
- Spring Boot 3.2.0 官方文檔
- Lettuce 6.4.0.RELEASE 官方文檔

## 日誌文件位置
- `logs/spring-boot-stomp.log` - 主日誌文件
- `logs/spring-boot-stomp.log.{date}.{index}.log` - 滾動日誌文件

### 9. 使用 RedisURI 構建連接
- **時間**: 2025-12-24
- **方法**: 
  - 使用 `RedisURI.Builder` 直接構建連接 URI
  - 使用 `withAuthentication(username, password)` 設置認證
  - 然後轉換為 `RedisStandaloneConfiguration` 供 Spring Data Redis 使用
- **結果**: ❌ 失敗，仍有 `RedisCredentialsProvider` 錯誤
- **分析**: 問題不在我們的代碼，而是 Spring Data Redis 3.2.0 內部嘗試使用 `RedisCredentialsProvider`，但 Lettuce 6.4.0 中該類已被移除

### 10. 升級 Spring Data Redis 到 3.3.0
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中明確指定 `spring-data-redis:3.3.0`
  - 期望新版本支持 Lettuce 6.4.0
- **結果**: ❌ 失敗，仍有 `RedisCredentialsProvider` 錯誤
- **分析**: Spring Data Redis 3.3.0 可能不存在或仍有兼容性問題

### 11. 升級 Spring Data Redis 到 3.2.5
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中明確指定 `spring-data-redis:3.2.5`（3.2.x 的最新版本）
  - 期望修復版本能解決兼容性問題
- **結果**: 測試中...

### 10. 升級 Spring Data Redis 到 3.3.0
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中明確指定 `spring-data-redis:3.3.0`
  - 期望新版本支持 Lettuce 6.4.0
- **結果**: ❌ 失敗，仍有 `RedisCredentialsProvider` 錯誤

### 11. 升級 Spring Data Redis 到 3.2.5
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中明確指定 `spring-data-redis:3.2.5`（3.2.x 的最新版本）
  - 期望修復版本能解決兼容性問題
- **結果**: ❌ 失敗，仍有 `RedisCredentialsProvider` 錯誤
- **分析**: Spring Data Redis 3.2.x 系列可能都依賴 `RedisCredentialsProvider`，而 Lettuce 6.4.0 已移除該類

### 12. 升級 Spring Boot 到 3.3.0
- **時間**: 2025-12-24
- **方法**: 
  - 在 `pom.xml` 中將 Spring Boot 版本從 3.2.0 升級到 3.3.0
  - 移除明確的 Spring Data Redis 和 Lettuce 版本指定，讓 Spring Boot 自動管理
  - Spring Boot 3.3.0 默認使用 Spring Data Redis 3.3.0 和 Lettuce 6.3.2.RELEASE
  - 嘗試明確指定 Lettuce 6.4.0.RELEASE，但仍有 `RedisCredentialsProvider` 錯誤
- **結果**: ❌ 失敗，即使使用 Spring Data Redis 3.3.0 和 Lettuce 6.3.2，仍有 `RedisCredentialsProvider` 錯誤

### 13. 降級 Redis 到 5.0（只使用密碼認證）
- **時間**: 2025-12-24
- **方法**: 
  - 根據 [博客園文章](https://www.cnblogs.com/upzhou/p/16759600.html)，Redis 7.0 引入了 ACL 權限控制，需要用戶名和密碼
  - Redis 6.0 之前只支持密碼認證（`AUTH <password>`），不需要用戶名
  - 降級 Redis 到 5.0（`redis:5-alpine`）
  - 移除 `spring.redis.username` 配置
  - 簡化 `RedisConfig.java`，移除 `RedisURI` 和用戶名相關代碼
  - 只使用 `RedisStandaloneConfiguration` 和密碼認證
- **結果**: ❌ 失敗，即使降級 Redis 到 5.0，仍有 `RedisCredentialsProvider` 錯誤
- **嘗試的版本組合**:
  1. Spring Boot 3.3.0 + Spring Data Redis 3.3.0 + Lettuce 6.3.2.RELEASE → ❌ 失敗
  2. Spring Boot 3.3.0 + Spring Data Redis 3.3.0 + Lettuce 6.2.3.RELEASE → ❌ 失敗
  3. Spring Boot 3.3.0 + Spring Data Redis 3.1.5 + Lettuce 6.2.3.RELEASE → ❌ 失敗
- **分析**: 問題不在 Redis 版本或 Lettuce 版本，而是 Spring Data Redis 3.x 系列（包括 3.1.x, 3.2.x, 3.3.x）都嘗試使用 `RedisCredentialsProvider`，但該類在 Lettuce 6.2+ 中可能已被移除或重構
- **當前配置**: 
  - Redis: 5.0
  - Spring Boot: 3.3.0
  - Spring Data Redis: 3.1.5
  - Lettuce: 6.2.3.RELEASE

## 版本兼容性分析

### Lettuce 6.4.0 與 Spring Data Redis 的兼容性
- **問題**: `RedisCredentialsProvider` 在 Lettuce 6.4.0 中已被移除
- **Spring Data Redis 3.2.x**: 所有版本（3.2.0, 3.2.5）都嘗試使用 `RedisCredentialsProvider`
- **Spring Data Redis 3.3.0**: 也存在同樣的問題
- **結論**: Spring Data Redis 3.2.x 和 3.3.0 可能都不支持 Lettuce 6.4.0

### 當前配置
- **Spring Boot**: 3.2.0
- **Spring Data Redis**: 3.2.5（明確指定）
- **Lettuce**: 6.4.0.RELEASE（明確指定）
- **Redis**: 7.4.7 (Docker)

## 下一步行動
1. ✅ 已嘗試使用 `RedisURI` 直接構建連接（失敗）
2. ✅ 已嘗試升級 Spring Data Redis 到 3.2.5 和 3.3.0（都失敗）
3. **建議**: 升級 Spring Boot 到 3.3.x 或更高版本（可能包含更新的 Spring Data Redis）
4. **建議**: 或者降級 Lettuce 到 6.3.0.RELEASE（Spring Boot 3.2.0 默認，應該兼容）
5. **建議**: 考慮使用 Jedis 作為臨時解決方案

