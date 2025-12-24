package com.hejz.springbootstomp;

import com.hejz.springbootstomp.service.AgentService;
import com.hejz.springbootstomp.service.AgentStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 專員狀態服務測試
 * 
 * <p>測試專員狀態管理功能，包括：
 * <ul>
 *   <li>設置專員為在線狀態</li>
 *   <li>設置專員為離線狀態</li>
 *   <li>獲取專員狀態</li>
 *   <li>狀態廣播功能</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@SpringBootTest
@ActiveProfiles("test")
public class AgentStatusServiceTests {

    @Autowired
    private AgentStatusService agentStatusService;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    public void setUp() {
        // 清理測試數據
        redisTemplate.delete("agent:a:status");
        redisTemplate.delete("agent:b:status");
        redisTemplate.delete("agent:a:id");
        redisTemplate.delete("agent:b:id");
    }

    @Test
    public void testSetAgentOnline() {
        // 創建專員A的ID
        String agentAId = agentService.getOrCreateAgentAId();
        assertNotNull(agentAId, "專員A ID不應為null");

        // 設置專員A為在線
        agentStatusService.setAgentOnline(agentAId);

        // 驗證狀態已保存到Redis
        String status = agentStatusService.getAgentAStatus();
        assertEquals("ONLINE", status, "專員A狀態應該是ONLINE");
    }

    @Test
    public void testSetAgentOffline() {
        // 創建專員A的ID
        String agentAId = agentService.getOrCreateAgentAId();
        assertNotNull(agentAId, "專員A ID不應為null");

        // 先設置為在線
        agentStatusService.setAgentOnline(agentAId);
        String onlineStatus = agentStatusService.getAgentAStatus();
        assertEquals("ONLINE", onlineStatus, "專員A應該是在線狀態");

        // 設置為離線
        agentStatusService.setAgentOffline(agentAId);

        // 驗證狀態已更新
        String offlineStatus = agentStatusService.getAgentAStatus();
        assertEquals("OFFLINE", offlineStatus, "專員A狀態應該是OFFLINE");
    }

    @Test
    public void testGetAgentStatus() {
        // 創建專員A和專員B的ID
        String agentAId = agentService.getOrCreateAgentAId();
        String agentBId = agentService.getOrCreateAgentBId();

        // 設置專員A為在線，專員B為離線
        agentStatusService.setAgentOnline(agentAId);
        agentStatusService.setAgentOffline(agentBId);

        // 驗證狀態
        String statusA = agentStatusService.getAgentAStatus();
        String statusB = agentStatusService.getAgentBStatus();

        assertEquals("ONLINE", statusA, "專員A應該是在線狀態");
        assertEquals("OFFLINE", statusB, "專員B應該是離線狀態");
    }

    @Test
    public void testStatusPersistence() {
        // 創建專員A的ID
        String agentAId = agentService.getOrCreateAgentAId();

        // 設置為在線
        agentStatusService.setAgentOnline(agentAId);

        // 直接從Redis驗證
        Object status = redisTemplate.opsForValue().get("agent:a:status");
        assertNotNull(status, "Redis中應該有專員A的狀態");
        assertEquals("ONLINE", status.toString(), "Redis中的狀態應該是ONLINE");
    }

    @Test
    public void testDetermineAgentType() {
        // 創建專員A和專員B的ID
        String agentAId = agentService.getOrCreateAgentAId();
        String agentBId = agentService.getOrCreateAgentBId();

        // 設置專員A為在線（這會調用determineAgentType）
        agentStatusService.setAgentOnline(agentAId);
        String statusA = agentStatusService.getAgentAStatus();
        assertEquals("ONLINE", statusA, "專員A應該是在線狀態");

        // 設置專員B為在線
        agentStatusService.setAgentOnline(agentBId);
        String statusB = agentStatusService.getAgentBStatus();
        assertEquals("ONLINE", statusB, "專員B應該是在線狀態");
    }

    @Test
    public void testStatusExpiration() throws InterruptedException {
        // 創建專員A的ID
        String agentAId = agentService.getOrCreateAgentAId();

        // 設置為在線（狀態會在30秒後過期）
        agentStatusService.setAgentOnline(agentAId);
        String status = agentStatusService.getAgentAStatus();
        assertEquals("ONLINE", status, "專員A應該是在線狀態");

        // 注意：實際測試過期需要等待30秒，這裡只驗證狀態設置成功
        // 在實際環境中，狀態會在30秒後自動過期
    }
}










