package com.hejz.springbootstomp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebSocket 訊息發送 REST 控制器
 * 
 * <p>此控制器提供 REST API 介面，允許外部系統（如後台管理系統、定時任務等）
 * 透過 HTTP POST 請求主動向 WebSocket 客戶端發送訊息。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>提供 HTTP API 發送公共訊息到所有連接的客戶端</li>
 *   <li>提供 HTTP API 發送私信給特定用戶</li>
 *   <li>支援系統主動推送通知和訊息</li>
 * </ul>
 * 
 * <p>使用場景：
 * <ul>
 *   <li>系統通知：伺服器主動推送系統公告、維護通知等</li>
 *   <li>定時任務：定時向用戶發送提醒訊息</li>
 *   <li>後台管理：管理員透過後台系統發送訊息給用戶</li>
 *   <li>第三方整合：外部系統需要向用戶發送訊息時</li>
 * </ul>
 * 
 * <p>與 MessageController 的區別：
 * <ul>
 *   <li>MessageController：處理來自 WebSocket 客戶端的訊息（用戶之間的訊息）</li>
 *   <li>WsController：處理來自 HTTP 請求的訊息（系統主動推送的訊息）</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.WsControllerTests
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@RestController
public class WsController {
    
    @Autowired
    private Notificationservice notificationservice;

    /**
     * 透過 HTTP POST 發送公共訊息
     * 
     * <p>此方法允許外部系統透過 HTTP POST 請求向所有連接的 WebSocket 客戶端
     * 發送公共訊息。訊息會透過 Redis Pub/Sub 機制同步到所有節點。
     * 
     * <p>請求格式：
     * <pre>
     * POST /sendMessage
     * Content-Type: application/x-www-form-urlencoded
     * 
     * message=您的訊息內容
     * </pre>
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收 HTTP POST 請求中的訊息參數</li>
     *   <li>調用 Notificationservice 的全局通知服務</li>
     *   <li>訊息透過 Redis Pub/Sub 發布到所有節點</li>
     *   <li>所有連接的客戶端收到訊息</li>
     * </ol>
     * 
     * <p>使用範例：
     * <pre>
     * curl -X POST http://localhost:8080/sendMessage -d "message=系統維護通知"
     * </pre>
     * 
     * @param message 要發送的訊息內容
     * 
     * @see com.hejz.springbootstomp.WsControllerTests#testSendMessage()
     * @see com.hejz.springbootstomp.WsControllerTests#testSendMessageWithEmptyContent()
     * @see com.hejz.springbootstomp.WsControllerTests#testSendMessagePublishesToRedis()
     */
    @PostMapping("sendMessage")
    public void sendMessage(String message){
        notificationservice.gloubNotificationservice(message);
    }
    
    /**
     * 透過 HTTP POST 發送個人私信
     * 
     * <p>此方法允許外部系統透過 HTTP POST 請求向特定用戶發送私信。
     * 私信直接發送給目標用戶，不經過 Redis。
     * 
     * <p>請求格式：
     * <pre>
     * POST /sendPrivateMessage
     * Content-Type: application/x-www-form-urlencoded
     * 
     * id=目標用戶ID&message=您的私信內容
     * </pre>
     * 
     * <p>處理流程：
     * <ol>
     *   <li>接收 HTTP POST 請求中的用戶 ID 和訊息參數</li>
     *   <li>調用 Notificationservice 的私信通知服務</li>
     *   <li>訊息直接發送給目標用戶的 WebSocket 連接</li>
     * </ol>
     * 
     * <p>使用範例：
     * <pre>
     * curl -X POST http://localhost:8080/sendPrivateMessage -d "id=user123&message=您的訂單已處理"
     * </pre>
     * 
     * <p>注意事項：
     * <ul>
     *   <li>目標用戶必須已建立 WebSocket 連接</li>
     *   <li>如果用戶未連接，訊息將無法送達</li>
     *   <li>用戶 ID 必須與 WebSocket 握手時分配的 Principal 名稱一致</li>
     * </ul>
     * 
     * @param id 目標用戶的唯一識別碼（與 WebSocket 握手時分配的 Principal 名稱一致）
     * @param message 要發送的私信內容
     * 
     * @see com.hejz.springbootstomp.WsControllerTests#testSendPrivateMessage()
     * @see com.hejz.springbootstomp.WsControllerTests#testSendPrivateMessageWithInvalidId()
     * @see com.hejz.springbootstomp.WsControllerTests#testSendPrivateMessageWithEmptyMessage()
     */
    @PostMapping("sendPrivateMessage")
    public void sendMessage(String id, String message){
        notificationservice.privateNotificationservice(id, message);
    }
}
