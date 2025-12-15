package com.hejz.springbootstomp;


import com.hejz.springbootstomp.dto.Message;
import com.hejz.springbootstomp.dto.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
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
@Controller
public class MessageController {

    @Autowired
    private RedisMessagePublisher redisPublisher;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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
    public void message(final Message message) throws InterruptedException {
        // 對訊息內容進行 HTML 轉義，防止 XSS 攻擊
        String escapedContent = HtmlUtils.htmlEscape(message.getContent());
        // 發布到 Redis，Redis 監聽器會轉發到所有節點的 WebSocket 客戶端
        redisPublisher.publish(escapedContent);
    }

    /**
     * 處理個人私信訊息
     * 
     * <p>此方法接收來自 WebSocket 客戶端的私信訊息，直接發送給指定的接收者。
     * 私信不經過 Redis，僅在單一節點內處理，確保訊息即時性和隱私性。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收客戶端發送的私信（透過 STOMP 協議，目標：ws/privateMessage）</li>
     *   <li>對訊息內容進行 HTML 轉義</li>
     *   <li>從訊息物件中提取接收者 ID（優先使用 id，其次使用 recipient）</li>
     *   <li>構建回應訊息，包含發送者資訊</li>
     *   <li>直接發送給目標用戶的 WebSocket 連接</li>
     * </ol>
     * 
     * <p>接收者識別：
     * <ul>
     *   <li>優先使用 message.getId() 作為接收者 ID</li>
     *   <li>如果 id 為空，則使用 message.getRecipient()</li>
     *   <li>如果兩者都為空，則發送給發送者自己（用於測試）</li>
     * </ul>
     * 
     * <p>注意事項：
     * <ul>
     *   <li>私信僅在單一節點內有效，不支援跨節點私信</li>
     *   <li>接收者必須與發送者在同一節點上連接</li>
     *   <li>如需跨節點私信，需要額外的路由機制</li>
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
        // 對訊息內容進行 HTML 轉義，防止 XSS 攻擊
        String escapedContent = HtmlUtils.htmlEscape(message.getContent());
        // 構建回應訊息，包含發送者資訊
        String responseContent = "用戶：" + principal.getName() + "發送的信息：" + escapedContent;
        
        // 從 message 中獲取接收者 ID（優先使用 id，其次使用 recipient）
        String recipient = message.getId() != null ? message.getId() : 
                          (message.getRecipient() != null ? message.getRecipient() : principal.getName());
        ResponseMessage responseMessage = new ResponseMessage(responseContent);
        // 直接發送給目標用戶，不經過 Redis
        messagingTemplate.convertAndSendToUser(recipient, "/topic/privateMessage", responseMessage);
    }
}
