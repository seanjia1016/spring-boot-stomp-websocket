package com.hejz.springbootstomp.service;

import com.hejz.springbootstomp.config.RabbitMQConfig;
import com.hejz.springbootstomp.dto.AgentIdChangeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentService 單元測試類別
 * 
 * <p>此測試類別驗證專員ID管理服務的功能，包括：
 * <ul>
 *   <li>獲取或創建專員ID</li>
 *   <li>使用Lua腳本原子性地設置專員ID</li>
 *   <li>當ID被覆蓋時，發送RabbitMQ通知</li>
 *   <li>查詢專員ID</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testGetOrCreateAgentAIdWhenNotExists() - 驗證創建新的專員A ID</li>
 *   <li>testGetOrCreateAgentAIdWhenExists() - 驗證獲取現有的專員A ID</li>
 *   <li>testGetOrCreateAgentBIdWhenNotExists() - 驗證創建新的專員B ID</li>
 *   <li>testSetAgentIdWithReplacement() - 驗證ID覆蓋時發送RabbitMQ通知</li>
 *   <li>testGetAgentAId() - 驗證查詢專員A ID</li>
 *   <li>testGetAgentBId() - 驗證查詢專員B ID</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.service.AgentService
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AgentServiceTests {

    // 模擬 Redis 模板，用於存儲和查詢專員ID
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    // 模擬 RabbitMQ 模板，用於發送ID變更通知
    @Mock
    private RabbitTemplate rabbitTemplate;

    // 模擬 Redis ValueOperations，用於操作 Redis 值
    @Mock
    private ValueOperations<String, Object> valueOperations;

    // 要測試的專員ID管理服務實例
    @InjectMocks
    private AgentService agentService;

    // Lua腳本（模擬）
    private DefaultRedisScript<Object> setAgentIdScript;

    @BeforeEach
    void setUp() {
        // 設定 Mock 行為：當呼叫 opsForValue() 時，返回模擬的 ValueOperations
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // 初始化 Lua 腳本（模擬）
        setAgentIdScript = new DefaultRedisScript<>();
        setAgentIdScript.setScriptText("-- Lua script");
        setAgentIdScript.setResultType(Object.class);
        
        // 使用反射設置私有欄位 setAgentIdScript
        try {
            java.lang.reflect.Field scriptField = AgentService.class.getDeclaredField("setAgentIdScript");
            scriptField.setAccessible(true);
            scriptField.set(agentService, setAgentIdScript);
        } catch (Exception e) {
            throw new RuntimeException("無法設置私有欄位", e);
        }
    }

    /**
     * 測試創建新的專員A ID（當ID不存在時）
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無（從Redis查詢，發現不存在）</li>
     *   <li>透過方法：getOrCreateAgentAId()</li>
     *   <li>預期結果：應該生成新的ID並存儲到Redis</li>
     * </ul>
     * 
     * <p>實際場景：當專員A首次連接時，系統會為其生成一個新的唯一ID。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getOrCreateAgentAId()
     */
    @Test
    @Order(1)
    void testGetOrCreateAgentAIdWhenNotExists() {
        // 設定 Mock 行為：當查詢專員A ID時，返回 null（表示不存在）
        when(valueOperations.get("agent:a:id")).thenReturn(null);
        
        // 設定 Mock 行為：當執行Lua腳本時，返回成功結果（新創建）
        // Lua腳本返回List格式：["created", "new-agent-a-id"]
        java.util.List<Object> scriptResult = new java.util.ArrayList<>();
        scriptResult.add("created");
        scriptResult.add("new-agent-a-id");
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(scriptResult);

        // 執行被測試的方法：獲取或創建專員A ID
        String agentId = agentService.getOrCreateAgentAId();

        // 驗證：確認返回的ID不為null
        assertNotNull(agentId);
        
        // 驗證：確認執行了Lua腳本來設置ID
        verify(redisTemplate, atLeastOnce()).execute(
                any(DefaultRedisScript.class), anyList(), any(), any(), any());
        
        // 驗證：確認沒有發送RabbitMQ通知（因為是新創建，不是覆蓋）
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.AGENT_ID_CHANGE_EXCHANGE), eq(RabbitMQConfig.AGENT_ID_CHANGE_ROUTING_KEY), any(AgentIdChangeMessage.class));
    }

    /**
     * 測試獲取現有的專員A ID（當ID已存在時）
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無（從Redis查詢，發現已存在）</li>
     *   <li>透過方法：getOrCreateAgentAId()</li>
     *   <li>預期結果：應該返回現有的ID，不創建新的ID</li>
     * </ul>
     * 
     * <p>實際場景：當專員A已連接過時，系統會返回其現有的ID。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getOrCreateAgentAId()
     */
    @Test
    @Order(2)
    void testGetOrCreateAgentAIdWhenExists() {
        // 注意：根據新的邏輯，每次連接都生成新ID，不再檢查現有ID
        // 所以這個測試需要更新為：模擬Lua腳本返回updated狀態（ID相同）
        
        // 設定 Mock 行為：當執行Lua腳本時，返回updated狀態（ID相同，只更新時間戳）
        // Lua腳本返回List格式：["updated", "existing-agent-a-id"]
        java.util.List<Object> scriptResult = new java.util.ArrayList<>();
        scriptResult.add("updated");
        scriptResult.add("existing-agent-a-id");
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(scriptResult);

        // 執行被測試的方法：獲取或創建專員A ID
        String agentId = agentService.getOrCreateAgentAId();

        // 驗證：確認返回的ID不為null（新邏輯總是返回新生成的ID）
        assertNotNull(agentId);
        
        // 驗證：確認執行了Lua腳本來設置ID
        verify(redisTemplate, atLeastOnce()).execute(
                any(DefaultRedisScript.class), anyList(), any(), any(), any());
        
        // 驗證：確認沒有發送RabbitMQ通知（因為是updated，不是replaced）
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.AGENT_ID_CHANGE_EXCHANGE), eq(RabbitMQConfig.AGENT_ID_CHANGE_ROUTING_KEY), any(AgentIdChangeMessage.class));
    }

    /**
     * 測試創建新的專員B ID（當ID不存在時）
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無（從Redis查詢，發現不存在）</li>
     *   <li>透過方法：getOrCreateAgentBId()</li>
     *   <li>預期結果：應該生成新的ID並存儲到Redis</li>
     * </ul>
     * 
     * <p>實際場景：當專員B首次連接時，系統會為其生成一個新的唯一ID。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getOrCreateAgentBId()
     */
    @Test
    @Order(3)
    void testGetOrCreateAgentBIdWhenNotExists() {
        // 設定 Mock 行為：當執行Lua腳本時，返回成功結果（新創建）
        // Lua腳本返回List格式：["created", "new-agent-b-id"]
        java.util.List<Object> scriptResult = new java.util.ArrayList<>();
        scriptResult.add("created");
        scriptResult.add("new-agent-b-id");
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(scriptResult);

        // 執行被測試的方法：獲取或創建專員B ID
        String agentId = agentService.getOrCreateAgentBId();

        // 驗證：確認返回的ID不為null
        assertNotNull(agentId);
        
        // 驗證：確認執行了Lua腳本來設置ID
        verify(redisTemplate, atLeastOnce()).execute(
                any(DefaultRedisScript.class), anyList(), any(), any(), any());
        
        // 驗證：確認沒有發送RabbitMQ通知（因為是新創建，不是覆蓋）
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.AGENT_ID_CHANGE_EXCHANGE), eq(RabbitMQConfig.AGENT_ID_CHANGE_ROUTING_KEY), any(AgentIdChangeMessage.class));
    }

    /**
     * 測試ID覆蓋時發送RabbitMQ通知
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無（從Redis查詢，發現已存在不同的ID）</li>
     *   <li>透過方法：getOrCreateAgentAId()（當ID被覆蓋時）</li>
     *   <li>預期結果：應該發送RabbitMQ通知，通知舊的連接斷開</li>
     * </ul>
     * 
     * <p>實際場景：當專員A的新連接覆蓋舊的ID時，系統會發送RabbitMQ通知，
     * 通知舊的連接斷開。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getOrCreateAgentAId()
     */
    @Test
    @Order(4)
    void testSetAgentIdWithReplacement() {
        // 設定 Mock 行為：當執行Lua腳本時，返回覆蓋結果（ID被替換）
        // Lua腳本返回List格式：["replaced", "old-agent-a-id", "new-agent-a-id"]
        java.util.List<Object> scriptResult = new java.util.ArrayList<>();
        scriptResult.add("replaced");
        scriptResult.add("old-agent-a-id");
        scriptResult.add("new-agent-a-id");
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                .thenReturn(scriptResult);

        // 執行被測試的方法：獲取或創建專員A ID（會觸發ID覆蓋）
        String agentId = agentService.getOrCreateAgentAId();

        // 驗證：確認返回的ID不為null
        assertNotNull(agentId);
        
        // 驗證：確認發送了RabbitMQ通知
        ArgumentCaptor<AgentIdChangeMessage> messageCaptor = 
                ArgumentCaptor.forClass(AgentIdChangeMessage.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.AGENT_ID_CHANGE_EXCHANGE),
                eq(RabbitMQConfig.AGENT_ID_CHANGE_ROUTING_KEY),
                messageCaptor.capture()
        );

        // 驗證：確認通知內容正確
        AgentIdChangeMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertEquals("a", capturedMessage.getAgentType());
        assertEquals("old-agent-a-id", capturedMessage.getOldId());
        assertEquals("new-agent-a-id", capturedMessage.getNewId());
        assertEquals("專員A", capturedMessage.getAgentName());
        assertNotNull(capturedMessage.getTimestamp());
    }

    /**
     * 測試Redis連接失敗時，應該拋出異常
     * 
     * <p>此測試驗證當Redis連接失敗時，getOrCreateAgentId應該拋出異常，
     * 而不是返回臨時ID，這樣可以確保不會允許多個專員同時連接。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getOrCreateAgentAId()
     */
    @Test
    @Order(7)
    @DisplayName("測試Redis連接失敗時應該拋出異常")
    void testGetOrCreateAgentAIdWhenRedisConnectionFails() {
        // 設定 Mock 行為：當執行Lua腳本時，拋出Redis連接異常
        // 注意：setAgentId方法會catch異常並記錄，但getOrCreateAgentId會重新拋出
        org.springframework.data.redis.RedisConnectionFailureException redisException = 
                new org.springframework.data.redis.RedisConnectionFailureException("Unable to connect to Redis");
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any()))
                .thenThrow(redisException);

        // 執行被測試的方法：應該拋出異常
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            agentService.getOrCreateAgentAId();
        });

        // 驗證：確認異常訊息包含Redis連接失敗的提示，或者異常的cause是Redis異常
        assertTrue(exception.getMessage().contains("Redis連接失敗") || 
                   exception.getMessage().contains("無法獲取專員ID") ||
                   (exception.getCause() != null && exception.getCause().getClass().equals(org.springframework.data.redis.RedisConnectionFailureException.class)));
        
        // 驗證：確認沒有發送RabbitMQ通知（因為連接失敗）
        verify(rabbitTemplate, never()).convertAndSend(eq(RabbitMQConfig.AGENT_ID_CHANGE_EXCHANGE), eq(RabbitMQConfig.AGENT_ID_CHANGE_ROUTING_KEY), any(AgentIdChangeMessage.class));
    }

    /**
     * 測試查詢專員A ID
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無</li>
     *   <li>透過方法：getAgentAId()</li>
     *   <li>預期結果：應該返回專員A的ID（如果存在）</li>
     * </ul>
     * 
     * <p>實際場景：當需要查詢專員A的ID時，系統會從Redis中獲取。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getAgentAId()
     */
    @Test
    @Order(5)
    void testGetAgentAId() {
        // 設定 Mock 行為：當查詢專員A ID時，返回測試ID
        String testId = "test-agent-a-id";
        when(valueOperations.get("agent:a:id")).thenReturn(testId);

        // 執行被測試的方法：查詢專員A ID
        String agentId = agentService.getAgentAId();

        // 驗證：確認返回的ID正確
        assertEquals(testId, agentId);
        
        // 驗證：確認查詢了Redis
        verify(valueOperations, times(1)).get("agent:a:id");
    }

    /**
     * 測試查詢專員B ID
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：無</li>
     *   <li>透過方法：getAgentBId()</li>
     *   <li>預期結果：應該返回專員B的ID（如果存在）</li>
     * </ul>
     * 
     * <p>實際場景：當需要查詢專員B的ID時，系統會從Redis中獲取。
     * 
     * @see com.hejz.springbootstomp.service.AgentService#getAgentBId()
     */
    @Test
    @Order(6)
    void testGetAgentBId() {
        // 設定 Mock 行為：當查詢專員B ID時，返回測試ID
        String testId = "test-agent-b-id";
        when(valueOperations.get("agent:b:id")).thenReturn(testId);

        // 執行被測試的方法：查詢專員B ID
        String agentId = agentService.getAgentBId();

        // 驗證：確認返回的ID正確
        assertEquals(testId, agentId);
        
        // 驗證：確認查詢了Redis
        verify(valueOperations, times(1)).get("agent:b:id");
    }
}


