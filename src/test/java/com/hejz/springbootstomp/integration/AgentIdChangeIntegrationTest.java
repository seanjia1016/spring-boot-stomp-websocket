package com.hejz.springbootstomp.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejz.springbootstomp.service.AgentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 專員ID變更集成測試
 * 
 * <p>此測試類別驗證專員ID變更的完整流程，包括：
 * <ul>
 *   <li>第一個專員A連接並獲取ID</li>
 *   <li>第二個專員A連接並覆蓋第一個專員A的ID</li>
 *   <li>第一個專員A的ID檢查應該返回無效</li>
 *   <li>驗證RabbitMQ通知機制</li>
 * </ul>
 * 
 * <p>測試場景：
 * <ol>
 *   <li>清除Redis中的專員A ID（確保測試環境乾淨）</li>
 *   <li>第一個專員A獲取ID（例如：id1）</li>
 *   <li>第二個專員A獲取ID，覆蓋第一個（id1 -> id2）</li>
 *   <li>驗證第一個專員A的ID已無效</li>
 *   <li>驗證第二個專員A的ID有效</li>
 * </ol>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AgentIdChangeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AgentService agentService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String baseUrl;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        objectMapper = new ObjectMapper();
        
        // 清除Redis中的專員A ID，確保測試環境乾淨
        redisTemplate.delete("agent:a:id");
        redisTemplate.delete("agent:a:id:info");
    }

    /**
     * 測試專員ID變更完整流程
     * 
     * <p>測試步驟：
     * <ol>
     *   <li>第一個專員A獲取ID（通過AgentService）</li>
     *   <li>驗證第一個專員A的ID已存儲在Redis中</li>
     *   <li>第二個專員A獲取ID，覆蓋第一個專員A的ID</li>
     *   <li>驗證第二個專員A的ID已更新到Redis中</li>
     *   <li>驗證第一個專員A的ID已變更（通過API檢查）</li>
     * </ol>
     * 
     * <p>預期結果：
     * <ul>
     *   <li>第一個專員A獲取ID（例如：id1）</li>
     *   <li>第二個專員A獲取ID，覆蓋第一個（id1 -> id2）</li>
     *   <li>第一個專員A的ID檢查應該返回isValid=false</li>
     *   <li>RabbitMQ應該會收到ID變更通知</li>
     * </ul>
     */
    @Test
    @Order(1)
    @DisplayName("測試專員A ID變更流程：第一個專員A應該在第二個專員A連接後ID變為無效")
    void testAgentAIdChangeFlow() throws Exception {
        // 步驟1：第一個專員A獲取ID（通過AgentService）
        String firstAgentId = agentService.getOrCreateAgentAId();
        assertNotNull(firstAgentId, "第一個專員A應該獲取到ID");
        System.out.println("第一個專員A ID: " + firstAgentId);
        
        // 等待一下，確保ID已存儲
        Thread.sleep(1000);
        
        // 步驟2：驗證第一個專員A的ID已存儲在Redis中
        String currentAgentId = agentService.getAgentAId();
        assertNotNull(currentAgentId, "Redis中應該有專員A的ID");
        assertEquals(firstAgentId, currentAgentId, "第一個專員A的ID應該存儲在Redis中");
        
        // 步驟3：驗證第一個專員A的ID是有效的（通過API檢查）
        boolean isValid = checkAgentIdValidity("a", firstAgentId);
        assertTrue(isValid, "第一個專員A的ID應該是有效的");
        
        // 步驟4：第二個專員A獲取ID，覆蓋第一個專員A的ID
        String secondAgentId = agentService.getOrCreateAgentAId();
        assertNotNull(secondAgentId, "第二個專員A應該獲取到ID");
        System.out.println("第二個專員A ID: " + secondAgentId);
        assertNotEquals(firstAgentId, secondAgentId, "兩個專員A的ID應該不同");
        
        // 等待一下，確保ID已更新，RabbitMQ通知已發送
        Thread.sleep(3000);
        
        // 步驟5：驗證第二個專員A的ID已更新到Redis中
        String newAgentId = agentService.getAgentAId();
        assertNotNull(newAgentId, "Redis中應該有專員A的ID");
        assertEquals(secondAgentId, newAgentId, "第二個專員A的ID應該存儲在Redis中");
        assertNotEquals(firstAgentId, newAgentId, "第二個專員A的ID應該與第一個不同");
        
        // 步驟6：驗證第一個專員A的ID已變更（通過API檢查）
        boolean firstAgentIdValid = checkAgentIdValidity("a", firstAgentId);
        assertFalse(firstAgentIdValid, "第一個專員A的ID應該已經無效");
        
        // 步驟7：驗證第二個專員A的ID是有效的
        boolean secondAgentIdValid = checkAgentIdValidity("a", secondAgentId);
        assertTrue(secondAgentIdValid, "第二個專員A的ID應該是有效的");
        
        System.out.println("✓ 測試通過：第一個專員A的ID已變更，第二個專員A的ID已生效");
    }

    /**
     * 測試專員ID變更後，舊ID應該無效
     * 
     * <p>此測試驗證當第二個專員A連接時，第一個專員A的ID會變為無效。
     */
    @Test
    @Order(2)
    @DisplayName("測試專員ID變更後，舊ID應該無效")
    void testAgentIdInvalidation() throws Exception {
        // 第一個專員A獲取ID
        String firstAgentId = agentService.getOrCreateAgentAId();
        assertNotNull(firstAgentId);
        
        // 等待ID存儲
        Thread.sleep(500);
        
        // 驗證第一個專員A的ID有效
        assertTrue(checkAgentIdValidity("a", firstAgentId));
        
        // 第二個專員A獲取ID，覆蓋第一個
        String secondAgentId = agentService.getOrCreateAgentAId();
        assertNotNull(secondAgentId);
        assertNotEquals(firstAgentId, secondAgentId);
        
        // 等待ID更新和RabbitMQ通知
        Thread.sleep(2000);
        
        // 驗證第一個專員A的ID無效
        assertFalse(checkAgentIdValidity("a", firstAgentId));
        
        // 驗證第二個專員A的ID有效
        assertTrue(checkAgentIdValidity("a", secondAgentId));
    }

    /**
     * 檢查專員ID是否有效
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @param agentId 專員ID
     * @return true如果ID有效，false如果ID無效
     */
    private boolean checkAgentIdValidity(String agentType, String agentId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/agent/" + agentType + "/check/" + agentId))
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonNode jsonNode = objectMapper.readTree(response.body());
            return jsonNode.get("isValid").asBoolean();
        }
        
        return false;
    }
}

