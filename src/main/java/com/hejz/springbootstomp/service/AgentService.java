package com.hejz.springbootstomp.service;

import com.hejz.springbootstomp.config.RabbitMQConfig;
import com.hejz.springbootstomp.dto.AgentIdChangeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 專員ID管理服務
 * 
 * <p>此服務負責管理專員A和專員B的ID，使用Redis存儲並確保原子性操作。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>為專員A和專員B生成並存儲唯一ID</li>
 *   <li>使用Lua腳本確保原子性操作</li>
 *   <li>如果ID改變則覆蓋舊的ID</li>
 *   <li>提供API查詢專員ID</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Service
public class AgentService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private DefaultRedisScript<List> setAgentIdScript;

    // Redis key 前綴
    private static final String AGENT_A_KEY = "agent:a:id";
    private static final String AGENT_B_KEY = "agent:b:id";
    private static final String AGENT_A_INFO_KEY = "agent:a:id:info";
    private static final String AGENT_B_INFO_KEY = "agent:b:id:info";

    @PostConstruct
    public void init() {
        // 載入Lua腳本
        try {
            ClassPathResource resource = new ClassPathResource("lua/set_agent_id.lua");
            String script = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            setAgentIdScript = new DefaultRedisScript<>();
            setAgentIdScript.setScriptText(script);
            // 設置返回類型為List，因為Lua腳本返回的是表（table），會被轉換為List
            setAgentIdScript.setResultType(List.class);
            log.info("專員ID管理服務已初始化，Lua腳本已載入");
        } catch (IOException e) {
            log.error("無法載入 set_agent_id.lua 腳本: {}", e.getMessage(), e);
            throw new RuntimeException("無法載入專員ID管理腳本", e);
        }
    }

    /**
     * 獲取或創建專員A的ID
     * 
     * @return 專員A的ID
     */
    public String getOrCreateAgentAId() {
        return getOrCreateAgentId("a", "專員A", AGENT_A_KEY);
    }

    /**
     * 獲取或創建專員B的ID
     * 
     * @return 專員B的ID
     */
    public String getOrCreateAgentBId() {
        return getOrCreateAgentId("b", "專員B", AGENT_B_KEY);
    }

    /**
     * 獲取或創建專員ID（內部方法）
     * 
     * <p>重要：每次連接都會生成新的ID並調用setAgentId，這樣可以觸發ID覆蓋邏輯。
     * 如果Redis中已有ID，Lua腳本會檢測到ID不同並返回replaced狀態，觸發RabbitMQ通知。
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @param agentName 專員名稱（"專員A" 或 "專員B"）
     * @param key Redis key
     * @return 專員ID
     */
    private String getOrCreateAgentId(String agentType, String agentName, String key) {
        try {
            // 每次連接都生成新的ID，然後調用setAgentId
            // 這樣可以確保：
            // 1. 如果Redis中沒有ID，會創建新的（返回created狀態）
            // 2. 如果Redis中已有ID且不同，會覆蓋舊的（返回replaced狀態，觸發RabbitMQ通知）
            // 3. 如果Redis中已有ID且相同，只更新時間戳（返回updated狀態）
            String newId = UUID.randomUUID().toString().replaceAll("-", "");
            log.info("為{}生成新ID: {}", agentName, newId);
            
            // 使用Lua腳本原子性地設置ID（會自動處理覆蓋邏輯）
            setAgentId(key, newId, agentName);
            
            // 返回新生成的ID（即使被覆蓋，也返回新的ID）
            return newId;
        } catch (Exception e) {
            log.error("獲取或創建{}ID時發生錯誤: {}", agentName, e.getMessage(), e);
            // 如果Redis完全無法連接，拋出異常，不允許連接
            // 這樣可以確保Redis連接失敗時，不會允許多個專員連接
            throw new RuntimeException("無法獲取專員ID，Redis連接失敗。請檢查Redis服務是否正常運行。", e);
        }
    }

    /**
     * 使用Lua腳本原子性地設置專員ID
     * 
     * @param key Redis key
     * @param id 專員ID
     * @param agentName 專員名稱
     */
    private void setAgentId(String key, String id, String agentName) {
        try {
            long timestamp = System.currentTimeMillis();
            log.info("準備設置{}ID: key={}, id={}, timestamp={}", agentName, key, id, timestamp);
            
            List<Object> result = redisTemplate.execute(setAgentIdScript,
                    Collections.singletonList(key),
                    id, agentName, String.valueOf(timestamp));
            
            log.info("設置{}ID結果: {} (類型: {})", agentName, result, result != null ? result.getClass().getName() : "null");
            
            // 檢查是否ID被覆蓋，如果是則發送RabbitMQ訊息
            // Lua腳本返回的表（table）在Java中會被轉換為List
            if (result != null && !result.isEmpty()) {
                log.info("設置{}ID結果List大小: {}, 內容: {}", agentName, result.size(), result);
                // 詳細記錄每個元素，便於調試
                for (int i = 0; i < result.size(); i++) {
                    log.info("設置{}ID結果[{}]: {} (類型: {})", agentName, i, result.get(i), 
                            result.get(i) != null ? result.get(i).getClass().getName() : "null");
                }
                
                // Lua返回的table格式：{status, oldId, newId} 或 {status, id}
                if (result.size() >= 1) {
                    String status = result.get(0).toString().trim();
                    log.info("設置{}ID狀態: '{}' (長度: {})", agentName, status, status.length());
                    
                    if ("replaced".equals(status) && result.size() >= 3) {
                        // ID被覆蓋，發送RabbitMQ訊息通知
                        String oldId = result.get(1).toString().trim();
                        String newId = result.get(2).toString().trim();
                        String agentType = key.contains("agent:a") ? "a" : "b";
                        
                        log.info("專員{}ID被覆蓋: {} -> {}，準備發送RabbitMQ通知", agentName, oldId, newId);
                        sendAgentIdChangeNotification(agentType, oldId, newId, agentName, timestamp);
                    } else {
                        log.info("設置{}ID狀態為'{}'，不需要發送RabbitMQ通知（size={}）", agentName, status, result.size());
                        if (result.size() < 3) {
                            log.warn("設置{}ID結果List大小不足3，無法提取oldId和newId（size={}）", agentName, result.size());
                        }
                    }
                } else {
                    log.warn("設置{}ID結果List為空", agentName);
                }
            } else {
                log.warn("設置{}ID結果為null或空", agentName);
            }
        } catch (Exception e) {
            log.error("設置{}ID時發生錯誤: {}", agentName, e.getMessage(), e);
            // 重新拋出異常，讓getOrCreateAgentId可以處理
            throw e;
        }
    }

    /**
     * 發送專員ID變更通知到 RabbitMQ
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @param oldId 舊的專員ID
     * @param newId 新的專員ID
     * @param agentName 專員名稱
     * @param timestamp 變更時間戳
     */
    private void sendAgentIdChangeNotification(String agentType, String oldId, String newId, 
                                                String agentName, long timestamp) {
        try {
            AgentIdChangeMessage message = new AgentIdChangeMessage(
                    agentType, oldId, newId, agentName, timestamp);
            
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.AGENT_ID_CHANGE_EXCHANGE,
                    RabbitMQConfig.AGENT_ID_CHANGE_ROUTING_KEY,
                    message);
            
            log.info("已發送專員ID變更通知到RabbitMQ: {} ({} -> {})", agentName, oldId, newId);
        } catch (Exception e) {
            log.error("發送專員ID變更通知到RabbitMQ失敗: {}", e.getMessage(), e);
        }
    }

    /**
     * 更新專員時間戳（用於心跳檢測）
     * 
     * @param key Redis key
     * @param id 專員ID
     * @param agentName 專員名稱
     */
    private void updateAgentTimestamp(String key, String id, String agentName) {
        try {
            long timestamp = System.currentTimeMillis();
            redisTemplate.execute(setAgentIdScript,
                    Collections.singletonList(key),
                    id, agentName, String.valueOf(timestamp));
        } catch (Exception e) {
            log.warn("更新{}時間戳時發生錯誤: {}", agentName, e.getMessage());
        }
    }

    /**
     * 獲取專員A的ID（不創建，僅查詢）
     * 
     * @return 專員A的ID，如果不存在則返回null
     */
    public String getAgentAId() {
        return (String) redisTemplate.opsForValue().get(AGENT_A_KEY);
    }

    /**
     * 獲取專員B的ID（不創建，僅查詢）
     * 
     * @return 專員B的ID，如果不存在則返回null
     */
    public String getAgentBId() {
        return (String) redisTemplate.opsForValue().get(AGENT_B_KEY);
    }

    /**
     * 根據專員類型獲取ID
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @return 專員ID，如果不存在則返回null
     */
    public String getAgentId(String agentType) {
        if ("a".equalsIgnoreCase(agentType)) {
            return getAgentAId();
        } else if ("b".equalsIgnoreCase(agentType)) {
            return getAgentBId();
        }
        return null;
    }
}

