package com.hejz.springbootstomp;


import com.hejz.springbootstomp.dto.Message;
import com.hejz.springbootstomp.dto.PrivateMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.util.HtmlUtils;

import java.security.Principal;

/**
 * WebSocket 訊息控制器
 * 
 * <p>此控制器負責處理來自 WebSocket 客戶端的 STOMP 訊息，包括公共聊天訊息
 * 和個人私信。使用 Redis Pub/Sub 機制實現多節點部署時的訊息同步。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>處理公共聊天訊息：接收客戶端訊息並發布到 Redis，實現多節點廣播</li>
 *   <li>處理個人私信：直接發送給特定用戶，不經過 Redis</li>
 *   <li>訊息安全處理：對 HTML 內容進行轉義，防止 XSS 攻擊</li>
 * </ul>
 * 
 * <p>訊息流程：
 * <ol>
 *   <li>公共訊息：客戶端 → MessageController → Redis Pub/Sub → 所有節點的客戶端</li>
 *   <li>私信訊息：客戶端 → MessageController → 直接發送給目標用戶</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.MessageControllerTests
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Controller
public class MessageController {

    @Autowired
    private RedisMessagePublisher redisPublisher;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private com.hejz.springbootstomp.service.ChatMessageService chatMessageService;
    
    @Autowired
    private com.hejz.springbootstomp.service.AgentService agentService;

    /**
     * 處理公共聊天訊息
     * 
     * <p>此方法接收來自 WebSocket 客戶端的公共訊息，進行安全處理後發布到
     * Redis Pub/Sub 頻道。Redis 監聽器會將訊息轉發到所有節點的 WebSocket 客戶端。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收客戶端發送的訊息（透過 STOMP 協議，目標：ws/message）</li>
     *   <li>對訊息內容進行 HTML 轉義，防止 XSS 攻擊</li>
     *   <li>將訊息發布到 Redis /topic/chat 頻道</li>
     *   <li>Redis 監聽器自動將訊息轉發到所有連接的客戶端</li>
     * </ol>
     * 
     * <p>多節點支援：
     * <ul>
     *   <li>當應用程式部署在多個節點時，所有節點的客戶端都能收到此訊息</li>
     *   <li>訊息通過 Redis Pub/Sub 機制實現跨節點同步</li>
     * </ul>
     * 
     * @param message 客戶端發送的訊息物件，包含訊息內容（content）
     * @throws InterruptedException 如果執行緒被中斷（目前未使用，保留以備未來擴展）
     * 
     * @see com.hejz.springbootstomp.MessageControllerTests#testMessage()
     * @see com.hejz.springbootstomp.MessageControllerTests#testMessageWithHtmlEscape()
     * @see com.hejz.springbootstomp.MessageControllerTests#testMessagePublishesToRedis()
     */
    @MessageMapping("/message")
    public void message(final Principal principal, final Message message) throws InterruptedException {
        // 對訊息內容進行 HTML 轉義，防止 XSS 攻擊
        String escapedContent = HtmlUtils.htmlEscape(message.getContent());
        
        log.info("=== 公共訊息接收 ===");
        log.info("訊息內容: {}", escapedContent);
        log.info("發布到 Redis /topic/chat 頻道");
        
        // 發布到 Redis，Redis 監聽器會轉發到所有節點的 WebSocket 客戶端
        redisPublisher.publish(escapedContent);
        
        // 持久化訊息到 Redis（使用 Lua 腳本確保原子性）
        String senderId = principal != null ? principal.getName() : "system";
        chatMessageService.savePublicMessage(senderId, null, escapedContent);
        
        log.info("公共訊息已發布到 Redis 並持久化");
    }

    /**
     * 處理個人私信訊息
     * 
     * <p>此方法接收來自 WebSocket 客戶端的私信訊息，通過 Redis Pub/Sub 機制
     * 發送給指定的接收者，支援跨節點私信。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收客戶端發送的私信（透過 STOMP 協議，目標：ws/privateMessage）</li>
     *   <li>對訊息內容進行 HTML 轉義，防止 XSS 攻擊</li>
     *   <li>從訊息物件中提取接收者 ID（優先使用 id，其次使用 recipient）</li>
     *   <li>構建 PrivateMessage 物件，包含發送者、接收者和訊息內容</li>
     *   <li>發布到 Redis /topic/privateMessage 頻道</li>
     *   <li>Redis 監聽器接收後轉發給目標用戶（僅在目標用戶連接的節點）</li>
     * </ol>
     * 
     * <p>接收者識別：
     * <ul>
     *   <li>優先使用 message.getId() 作為接收者 ID</li>
     *   <li>如果 id 為空，則使用 message.getRecipient()</li>
     *   <li>如果兩者都為空，則發送給發送者自己（用於測試）</li>
     * </ul>
     * 
     * <p>多節點支援：
     * <ul>
     *   <li>通過 Redis Pub/Sub 實現跨節點私信</li>
     *   <li>所有節點都訂閱 /topic/privateMessage 頻道</li>
     *   <li>只有目標用戶連接的節點才會轉發訊息</li>
     * </ul>
     * 
     * @param principal 發送者的身份資訊，包含發送者的唯一 ID
     * @param message 私信訊息物件，包含：
     *                <ul>
     *                  <li>content: 訊息內容</li>
     *                  <li>id: 接收者 ID（優先使用）</li>
     *                  <li>recipient: 接收者 ID（備用）</li>
     *                </ul>
     * @throws InterruptedException 如果執行緒被中斷（目前未使用，保留以備未來擴展）
     * 
     * @see com.hejz.springbootstomp.MessageControllerTests#testPrivateMessage()
     * @see com.hejz.springbootstomp.MessageControllerTests#testPrivateMessageWithId()
     * @see com.hejz.springbootstomp.MessageControllerTests#testPrivateMessageWithRecipient()
     */
    @MessageMapping("/privateMessage")
    public void privateMessage(final Principal principal, final Message message) throws InterruptedException {
        log.info("=== 私信接收開始 ===");
        log.info("收到私信請求，發送者: {}", principal != null ? principal.getName() : "null");
        log.info("訊息物件: {}", message != null ? message.toString() : "null");
        
        if (message == null) {
            log.error("訊息物件為 null，無法處理");
            return;
        }
        
        if (message.getContent() == null || message.getContent().trim().isEmpty()) {
            log.error("訊息內容為空，無法處理");
            return;
        }
        
        // 對訊息內容進行 HTML 轉義，防止 XSS 攻擊
        String escapedContent = HtmlUtils.htmlEscape(message.getContent());
        
        // 判斷發送者是專員A還是專員B
        String senderName = "專員";
        try {
            String senderId = principal.getName();
            String agentAId = agentService.getAgentAId();
            String agentBId = agentService.getAgentBId();
            
            if (agentAId != null && agentAId.equals(senderId)) {
                senderName = "專員A";
            } else if (agentBId != null && agentBId.equals(senderId)) {
                senderName = "專員B";
            } else {
                senderName = "專員";
            }
        } catch (Exception e) {
            log.warn("無法判斷專員類型，使用默認值: {}", e.getMessage());
        }
        
        // 從 message 中獲取接收者 ID（優先使用 id，其次使用 recipient）
        String recipient = message.getId() != null ? message.getId() : 
                          (message.getRecipient() != null ? message.getRecipient() : principal.getName());
        
        log.info("=== 私信發送調試 ===");
        log.info("發送者ID: {}", principal.getName());
        log.info("發送者名稱: {}", senderName);
        log.info("接收者ID: {}", recipient);
        log.info("訊息內容: {}", escapedContent);
        
        try {
            // 構建 PrivateMessage 物件
            PrivateMessage privateMessage = new PrivateMessage();
            privateMessage.setType("private");
            privateMessage.setSenderId(principal.getName());
            privateMessage.setSenderName(senderName);
            privateMessage.setRecipientId(recipient);
            privateMessage.setContent(escapedContent);
            privateMessage.setTimestamp(System.currentTimeMillis());
            
            // 發布到 Redis /topic/privateMessage 頻道
            redisPublisher.publishPrivateMessage(privateMessage);
            log.info("私信已發布到 Redis /topic/privateMessage 頻道");
            
            // 持久化私信訊息到 Redis（使用 Lua 腳本確保原子性）
            chatMessageService.savePrivateMessage(principal.getName(), senderName, recipient, escapedContent);
            log.info("私信已持久化到 Redis");
        } catch (Exception e) {
            log.error("發送私信時發生錯誤: {}", e.getMessage(), e);
            throw e;
        }
        
        log.info("=== 私信處理完成 ===");
    }
}
