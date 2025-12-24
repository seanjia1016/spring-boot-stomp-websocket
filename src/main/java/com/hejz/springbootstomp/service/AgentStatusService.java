package com.hejz.springbootstomp.service;

import com.hejz.springbootstomp.dto.AgentStatusMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 專員狀態管理服務
 * 
 * <p>此服務負責管理專員A和專員B的線上狀態，包括：
 * <ul>
 *   <li>記錄專員的線上/離線狀態到 Redis</li>
 *   <li>當狀態改變時，廣播狀態更新給對方</li>
 *   <li>提供查詢專員狀態的API</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Service
public class AgentStatusService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AgentService agentService;

    // Redis key 前綴
    private static final String AGENT_A_STATUS_KEY = "agent:a:status";
    private static final String AGENT_B_STATUS_KEY = "agent:b:status";
    
    // 狀態值
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    
    // 狀態過期時間（秒）- 如果專員斷開連接，狀態會在30秒後過期
    private static final long STATUS_EXPIRE_SECONDS = 30;

    /**
     * 設置專員為在線狀態
     * 
     * <p>當專員連接時，將狀態標記為在線，並廣播狀態更新給對方。
     * 
     * @param userId 專員的用戶ID
     */
    public void setAgentOnline(String userId) {
        log.info("=== 設置專員為在線狀態 ===");
        log.info("用戶ID: {}", userId);
        
        String agentType = determineAgentType(userId);
        log.info("確定的專員類型: {}", agentType);
        
        if (agentType == null) {
            log.warn("⚠ 無法確定專員類型，用戶ID: {}", userId);
            // 嘗試從Redis獲取專員ID進行調試
            String agentAId = agentService.getAgentAId();
            String agentBId = agentService.getAgentBId();
            log.warn("當前Redis中的專員A ID: {}", agentAId);
            log.warn("當前Redis中的專員B ID: {}", agentBId);
            return;
        }

        String statusKey = getStatusKey(agentType);
        String agentName = "a".equals(agentType) ? "專員A" : "專員B";
        log.info("專員名稱: {}, 狀態Key: {}", agentName, statusKey);
        
        try {
            // 更新 Redis 中的狀態
            redisTemplate.opsForValue().set(statusKey, STATUS_ONLINE, STATUS_EXPIRE_SECONDS, TimeUnit.SECONDS);
            log.info("✓ {} 狀態已更新到Redis: ONLINE，用戶ID: {}", agentName, userId);
            
            // 驗證狀態是否成功寫入
            Object savedStatus = redisTemplate.opsForValue().get(statusKey);
            log.info("驗證Redis中的狀態: {}", savedStatus);
            
            // 廣播狀態更新給對方
            log.info("準備廣播{}狀態更新...", agentName);
            broadcastStatusUpdate(agentType, agentName, STATUS_ONLINE);
        } catch (Exception e) {
            log.error("更新專員狀態到Redis時發生錯誤: {}", e.getMessage(), e);
        }
    }

    /**
     * 設置專員為離線狀態
     * 
     * <p>當專員斷開連接時，將狀態標記為離線，並廣播狀態更新給對方。
     * 
     * @param userId 專員的用戶ID
     */
    public void setAgentOffline(String userId) {
        String agentType = determineAgentType(userId);
        if (agentType == null) {
            log.warn("無法確定專員類型，用戶ID: {}", userId);
            return;
        }

        String statusKey = getStatusKey(agentType);
        String agentName = "a".equals(agentType) ? "專員A" : "專員B";
        
        // 更新 Redis 中的狀態
        redisTemplate.opsForValue().set(statusKey, STATUS_OFFLINE, STATUS_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("{} 已離線，用戶ID: {}", agentName, userId);
        
        // 廣播狀態更新給對方
        broadcastStatusUpdate(agentType, agentName, STATUS_OFFLINE);
    }

    /**
     * 獲取專員狀態
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @return 專員狀態（ONLINE 或 OFFLINE），如果不存在則返回 null
     */
    public String getAgentStatus(String agentType) {
        String statusKey = getStatusKey(agentType);
        Object status = redisTemplate.opsForValue().get(statusKey);
        return status != null ? status.toString() : null;
    }

    /**
     * 獲取專員A的狀態
     * 
     * @return 專員A的狀態（ONLINE 或 OFFLINE），如果不存在則返回 null
     */
    public String getAgentAStatus() {
        return getAgentStatus("a");
    }

    /**
     * 獲取專員B的狀態
     * 
     * @return 專員B的狀態（ONLINE 或 OFFLINE），如果不存在則返回 null
     */
    public String getAgentBStatus() {
        return getAgentStatus("b");
    }

    /**
     * 廣播狀態更新給對方
     * 
     * <p>當專員狀態改變時，通過 WebSocket 廣播狀態更新給對方。
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @param agentName 專員名稱（"專員A" 或 "專員B"）
     * @param status 狀態（ONLINE 或 OFFLINE）
     */
    private void broadcastStatusUpdate(String agentType, String agentName, String status) {
        try {
            log.info("=== 廣播專員狀態更新 ===");
            log.info("專員類型: {}, 專員名稱: {}, 狀態: {}", agentType, agentName, status);
            
            AgentStatusMessage message = new AgentStatusMessage(agentType, agentName, status, System.currentTimeMillis());
            log.info("創建的狀態訊息: agentType={}, agentName={}, status={}, timestamp={}", 
                    message.getAgentType(), message.getAgentName(), message.getStatus(), message.getTimestamp());
            
            // 廣播到公共頻道，所有連接的客戶端都能收到
            String destination = "/topic/agentStatus";
            log.info("準備發送到頻道: {}", destination);
            messagingTemplate.convertAndSend(destination, message);
            
            log.info("✓ 已成功廣播{}狀態更新到頻道 {}: {}", agentName, destination, status);
        } catch (Exception e) {
            log.error("✗ 廣播狀態更新失敗: {}", e.getMessage(), e);
            log.error("錯誤堆疊:", e);
        }
    }

    /**
     * 根據用戶ID確定專員類型
     * 
     * @param userId 用戶ID
     * @return 專員類型（"a" 或 "b"），如果無法確定則返回 null
     */
    private String determineAgentType(String userId) {
        log.info("=== 確定專員類型 ===");
        log.info("用戶ID: {}", userId);
        
        // 從 Redis 獲取專員A和專員B的ID
        String agentAId = agentService.getAgentAId();
        String agentBId = agentService.getAgentBId();
        
        log.info("Redis中的專員A ID: {}", agentAId);
        log.info("Redis中的專員B ID: {}", agentBId);
        
        if (userId != null) {
            if (userId.equals(agentAId)) {
                log.info("✓ 匹配專員A，用戶ID: {}", userId);
                return "a";
            } else if (userId.equals(agentBId)) {
                log.info("✓ 匹配專員B，用戶ID: {}", userId);
                return "b";
            } else {
                log.warn("⚠ 用戶ID不匹配任何專員ID");
                log.warn("用戶ID: {}", userId);
                log.warn("專員A ID: {}", agentAId);
                log.warn("專員B ID: {}", agentBId);
            }
        } else {
            log.warn("⚠ 用戶ID為null");
        }
        
        return null;
    }

    /**
     * 獲取狀態 Redis key
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @return Redis key
     */
    private String getStatusKey(String agentType) {
        return "a".equalsIgnoreCase(agentType) ? AGENT_A_STATUS_KEY : AGENT_B_STATUS_KEY;
    }
}

