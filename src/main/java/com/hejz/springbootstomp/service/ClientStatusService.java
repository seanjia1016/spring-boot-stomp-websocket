package com.hejz.springbootstomp.service;

import com.hejz.springbootstomp.dto.ClientStatusCheckMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 客戶端狀態服務
 * 
 * <p>此服務類別負責管理 WebSocket 客戶端的線上狀態，包括：
 * <ul>
 *   <li>記錄客戶端最後心跳時間到 Redis</li>
 *   <li>檢查客戶端是否超時</li>
 *   <li>更新客戶端狀態</li>
 * </ul>
 * 
 * <p>工作流程：
 * <ol>
 *   <li>客戶端發送心跳 → 記錄最後訪問時間到 Redis</li>
 *   <li>發送延遲訊息到 RabbitMQ（延遲時間 > 心跳間隔）</li>
 *   <li>延遲時間到達 → 檢查 Redis 中的最後訪問時間</li>
 *   <li>如果超時 → 更新客戶端狀態為「未連接」</li>
 * </ol>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Service
public class ClientStatusService {

    /**
     * Redis 鍵前綴：客戶端最後心跳時間
     */
    private static final String REDIS_KEY_PREFIX = "client:heartbeat:";

    /**
     * Redis 鍵前綴：客戶端狀態
     */
    private static final String REDIS_STATUS_KEY_PREFIX = "client:status:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 心跳間隔時間（毫秒），從配置檔案讀取
     */
    @Value("${client.status.heartbeat.interval:30000}")
    private long heartbeatInterval;

    /**
     * 檢查延遲時間（毫秒），從配置檔案讀取
     */
    @Value("${client.status.check.delay:60000}")
    private long checkDelay;

    /**
     * 記錄客戶端心跳
     * 
     * <p>當客戶端發送心跳時，將最後訪問時間記錄到 Redis，
     * 並設定過期時間為檢查延遲時間的 2 倍，確保即使檢查失敗也能自動清理。
     * 
     * @param clientId 客戶端 ID
     * @return 記錄的心跳時間戳
     */
    public Long recordHeartbeat(String clientId) {
        long currentTime = System.currentTimeMillis();
        String key = REDIS_KEY_PREFIX + clientId;
        
        // 記錄最後心跳時間到 Redis，設定過期時間為檢查延遲時間的 2 倍
        redisTemplate.opsForValue().set(key, currentTime, checkDelay * 2, TimeUnit.MILLISECONDS);
        
        // 更新客戶端狀態為「在線」
        String statusKey = REDIS_STATUS_KEY_PREFIX + clientId;
        redisTemplate.opsForValue().set(statusKey, "ONLINE", checkDelay * 2, TimeUnit.MILLISECONDS);
        
        log.debug("記錄客戶端心跳：clientId={}, timestamp={}", clientId, currentTime);
        return currentTime;
    }

    /**
     * 檢查客戶端是否超時
     * 
     * <p>從 Redis 讀取客戶端最後心跳時間，與預期時間比較，
     * 如果超過心跳間隔時間，則認為客戶端已離線。
     * 
     * @param clientId 客戶端 ID
     * @param expectedLastHeartbeatTime 預期的最後心跳時間
     * @return true 如果客戶端超時，false 如果客戶端仍在線
     */
    public boolean checkClientTimeout(String clientId, Long expectedLastHeartbeatTime) {
        String key = REDIS_KEY_PREFIX + clientId;
        Object lastHeartbeatObj = redisTemplate.opsForValue().get(key);
        
        if (lastHeartbeatObj == null) {
            // Redis 中沒有記錄，表示客戶端已離線
            log.info("客戶端已離線（無心跳記錄）：clientId={}", clientId);
            updateClientStatus(clientId, "OFFLINE", expectedLastHeartbeatTime);
            return true;
        }
        
        // 處理不同的序列化格式（可能是 Long、Integer 或字串）
        Long lastHeartbeatTime;
        if (lastHeartbeatObj instanceof Long) {
            lastHeartbeatTime = (Long) lastHeartbeatObj;
        } else if (lastHeartbeatObj instanceof Integer) {
            lastHeartbeatTime = ((Integer) lastHeartbeatObj).longValue();
        } else {
            // 嘗試解析為字串
            try {
                lastHeartbeatTime = Long.parseLong(lastHeartbeatObj.toString());
            } catch (NumberFormatException e) {
                log.error("無法解析最後心跳時間：clientId={}, value={}", clientId, lastHeartbeatObj);
                updateClientStatus(clientId, "OFFLINE", expectedLastHeartbeatTime);
                return true;
            }
        }
        
        long currentTime = System.currentTimeMillis();
        long timeSinceLastHeartbeat = currentTime - lastHeartbeatTime;
        
        if (timeSinceLastHeartbeat > heartbeatInterval) {
            // 超過心跳間隔時間，認為客戶端已離線
            log.info("客戶端已超時：clientId={}, 最後心跳時間={}, 當前時間={}, 間隔={}ms", 
                    clientId, lastHeartbeatTime, currentTime, timeSinceLastHeartbeat);
            updateClientStatus(clientId, "OFFLINE", lastHeartbeatTime);
            return true;
        } else {
            // 客戶端仍在線
            log.debug("客戶端仍在線：clientId={}, 最後心跳時間={}, 間隔={}ms", 
                    clientId, lastHeartbeatTime, timeSinceLastHeartbeat);
            return false;
        }
    }

    /**
     * 更新客戶端狀態
     * 
     * @param clientId 客戶端 ID
     * @param status 狀態（ONLINE 或 OFFLINE）
     * @param lastHeartbeatTime 最後心跳時間
     */
    private void updateClientStatus(String clientId, String status, Long lastHeartbeatTime) {
        String statusKey = REDIS_STATUS_KEY_PREFIX + clientId;
        redisTemplate.opsForValue().set(statusKey, status, checkDelay * 2, TimeUnit.MILLISECONDS);
        
        log.info("更新客戶端狀態：clientId={}, status={}, lastHeartbeatTime={}", 
                clientId, status, lastHeartbeatTime);
        
        // 這裡可以擴展為將狀態寫入資料庫
        // TODO: 如果需要持久化，可以在這裡調用資料庫服務
    }

    /**
     * 獲取客戶端狀態
     * 
     * @param clientId 客戶端 ID
     * @return 客戶端狀態（ONLINE 或 OFFLINE），如果不存在則返回 null
     */
    public String getClientStatus(String clientId) {
        String statusKey = REDIS_STATUS_KEY_PREFIX + clientId;
        Object status = redisTemplate.opsForValue().get(statusKey);
        return status != null ? status.toString() : null;
    }
}

