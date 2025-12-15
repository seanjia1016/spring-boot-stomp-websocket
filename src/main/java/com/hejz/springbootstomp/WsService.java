package com.hejz.springbootstomp;

import com.hejz.springbootstomp.dto.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 訊息服務類別
 * 
 * <p>此服務類別提供 WebSocket 訊息發送的核心功能，包括公共訊息廣播和
 * 個人私信發送。使用 Redis Pub/Sub 機制實現多節點部署時的訊息同步。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>公共訊息通知：透過 Redis Pub/Sub 向所有節點的客戶端發送訊息</li>
 *   <li>個人私信發送：直接向特定用戶的 WebSocket 連接發送訊息</li>
 * </ul>
 * 
 * <p>與 Notificationservice 的關係：
 * <ul>
 *   <li>WsService：提供基礎的訊息發送功能</li>
 *   <li>Notificationservice：基於 WsService 的生產環境實作，提供更完整的服務</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.WsServiceTests
 * @see com.hejz.springbootstomp.Notificationservice
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Service
public class WsService {

    private SimpMessagingTemplate template;
    private RedisMessagePublisher redisPublisher;

    /**
     * 建構函式，注入必要的依賴
     * 
     * @param template Spring WebSocket 訊息模板，用於發送訊息給客戶端
     * @param redisPublisher Redis 訊息發布服務，用於多節點訊息同步
     */
    @Autowired
    public WsService(SimpMessagingTemplate template, RedisMessagePublisher redisPublisher){
        this.template = template;
        this.redisPublisher = redisPublisher;
    }

    /**
     * 發送公共訊息通知
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
     * @param message 要發送的訊息內容
     * 
     * @see com.hejz.springbootstomp.WsServiceTests#testNotify()
     * @see com.hejz.springbootstomp.WsServiceTests#testNotifyPublishesToRedis()
     * @see com.hejz.springbootstomp.WsServiceTests#testNotifyWithNullMessage()
     */
    public void notify(String message){
        // 發布到 Redis /topic/chat 頻道，實現多節點訊息同步
        redisPublisher.publish(message);
    }

    /**
     * 發送個人私信
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
     *   <li>路徑使用 /topic/privateMessage，不需要加 /user/ 前綴</li>
     *   <li>客戶端訂閱時需要使用 /user/topic/privateMessage</li>
     * </ul>
     * 
     * @param id 目標用戶的唯一識別碼（與 WebSocket 握手時分配的 Principal 名稱一致）
     * @param message 要發送的私信內容
     * 
     * @see com.hejz.springbootstomp.WsServiceTests#testPrivateNotify()
     * @see com.hejz.springbootstomp.WsServiceTests#testPrivateNotifyWithInvalidId()
     * @see com.hejz.springbootstomp.WsServiceTests#testPrivateNotifyWithNullMessage()
     */
    public void privateNotify(String id, String message){
        ResponseMessage responseMessage = new ResponseMessage(message);
        // 注意：使用 convertAndSendToUser 方法時，路徑不需要加 /user/ 前綴
        // 客戶端訂閱時需要使用 /user/topic/privateMessage
        template.convertAndSendToUser(id, "/topic/privateMessage", responseMessage);
    }
}
