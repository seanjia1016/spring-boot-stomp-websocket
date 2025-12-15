package com.hejz.springbootstomp;

import com.hejz.springbootstomp.dto.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * 伺服器端 WebSocket 通知服務
 * 
 * <p>此服務類別提供生產環境可用的 WebSocket 訊息通知功能，包括全局訊息廣播
 * 和個人私信發送。使用 Redis Pub/Sub 機制實現多節點部署時的訊息同步。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>全局通知服務：向所有連接的客戶端發送公共訊息</li>
 *   <li>私信通知服務：向特定用戶發送個人私信</li>
 * </ul>
 * 
 * <p>與 WsService 的關係：
 * <ul>
 *   <li>WsService：提供基礎的訊息發送功能</li>
 *   <li>Notificationservice：基於 WsService 的生產環境實作，提供更完整的服務</li>
 * </ul>
 * 
 * <p>使用場景：
 * <ul>
 *   <li>系統通知：伺服器主動推送系統公告、維護通知等</li>
 *   <li>用戶通知：向特定用戶發送個人訊息、提醒等</li>
 *   <li>多節點部署：透過 Redis 實現跨節點訊息同步</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.NotificationserviceTests
 * @see com.hejz.springbootstomp.WsService
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Service
public class Notificationservice {

    private final SimpMessagingTemplate template;
    private final RedisMessagePublisher redisPublisher;

    /**
     * 建構函式，注入必要的依賴
     * 
     * @param template Spring WebSocket 訊息模板，用於發送訊息給客戶端
     * @param redisPublisher Redis 訊息發布服務，用於多節點訊息同步
     */
    @Autowired
    private Notificationservice(SimpMessagingTemplate template, RedisMessagePublisher redisPublisher){
        this.template = template;
        this.redisPublisher = redisPublisher;
    }

    /**
     * 全局通知服務
     * 
     * <p>此方法將訊息發布到 Redis Pub/Sub 頻道，實現多節點訊息同步。
     * 所有連接的 WebSocket 客戶端都會收到此訊息。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收訊息內容</li>
     *   <li>透過 RedisMessagePublisher 發布到 Redis /topic/chat 頻道</li>
     *   <li>Redis 監聽器將訊息轉發到所有節點的 WebSocket 客戶端</li>
     * </ol>
     * 
     * <p>多節點支援：
     * <ul>
     *   <li>當應用程式部署在多個節點時，所有節點的客戶端都能收到訊息</li>
     *   <li>訊息通過 Redis Pub/Sub 機制實現跨節點同步</li>
     * </ul>
     * 
     * <p>使用範例：
     * <pre>
     * notificationservice.gloubNotificationservice("系統維護通知：將於今晚 22:00 進行維護");
     * </pre>
     * 
     * @param message 要發送的訊息內容
     * 
     * @see com.hejz.springbootstomp.NotificationserviceTests#testGloubNotificationservice()
     * @see com.hejz.springbootstomp.NotificationserviceTests#testGloubNotificationservicePublishesToRedis()
     * @see com.hejz.springbootstomp.NotificationserviceTests#testGloubNotificationserviceWithEmptyMessage()
     */
    public void gloubNotificationservice(String message){
        // 發布到 Redis，Redis 監聽器會轉發到所有節點的 WebSocket 客戶端
        redisPublisher.publish(message);
    }
    
    /**
     * 私信通知服務
     * 
     * <p>此方法直接向特定用戶的 WebSocket 連接發送私信，不經過 Redis。
     * 私信僅在單一節點內有效。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收目標用戶 ID 和訊息內容</li>
     *   <li>創建 ResponseMessage 物件</li>
     *   <li>使用 SimpMessagingTemplate 直接發送給目標用戶</li>
     * </ol>
     * 
     * <p>注意事項：
     * <ul>
     *   <li>目標用戶必須已建立 WebSocket 連接</li>
     *   <li>用戶 ID 必須與 WebSocket 握手時分配的 Principal 名稱一致</li>
     *   <li>如果用戶未連接，訊息將無法送達</li>
     * </ul>
     * 
     * <p>使用範例：
     * <pre>
     * notificationservice.privateNotificationservice("user123", "您的訂單已處理完成");
     * </pre>
     * 
     * @param id 目標用戶的唯一識別碼（與 WebSocket 握手時分配的 Principal 名稱一致）
     * @param message 要發送的私信內容
     * 
     * @see com.hejz.springbootstomp.NotificationserviceTests#testPrivateNotificationservice()
     * @see com.hejz.springbootstomp.NotificationserviceTests#testPrivateNotificationserviceWithInvalidId()
     * @see com.hejz.springbootstomp.NotificationserviceTests#testPrivateNotificationserviceWithEmptyMessage()
     */
    public void privateNotificationservice(String id, String message){
        ResponseMessage responseMessage = new ResponseMessage(message);
        // 私信直接發送，不需要通過 Redis
        template.convertAndSendToUser(id, "/topic/privateMessage", responseMessage);
    }
}
