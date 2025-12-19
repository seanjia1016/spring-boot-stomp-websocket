package com.hejz.springbootstomp;

import com.hejz.springbootstomp.dto.ResponseMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 訊息監聽器
 * 
 * <p>此監聽器類別實現 MessageListener 介面，用於監聽 Redis Pub/Sub 頻道
 * 的訊息。當收到訊息時，會將訊息轉發給所有連接的 WebSocket 客戶端。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>監聽 Redis /topic/chat 頻道的訊息</li>
 *   <li>將 Redis 訊息反序列化為 ResponseMessage 物件</li>
 *   <li>透過 SimpMessagingTemplate 轉發給 WebSocket 客戶端</li>
 * </ul>
 * 
 * <p>訊息流程：
 * <ol>
     *   <li>RedisMessagePublisher 發布訊息到 Redis /topic/chat 頻道</li>
     *   <li>RedisMessageListener 收到訊息通知</li>
     *   <li>將 JSON 字串反序列化為 ResponseMessage 物件</li>
     *   <li>透過 SimpMessagingTemplate 轉發到 WebSocket /topic/chat 頻道</li>
     *   <li>所有訂閱 /topic/chat 的客戶端收到訊息</li>
     * </ol>
 * 
 * <p>多節點支援：
 * <ul>
 *   <li>每個節點都有自己的 RedisMessageListener 實例</li>
 *   <li>所有節點都訂閱同一個 Redis 頻道</li>
 *   <li>當任一節點發布訊息時，所有節點的監聽器都會收到</li>
 *   <li>每個節點將訊息轉發給自己連接的客戶端</li>
 * </ul>
 * 
 * <p>錯誤處理：
 * <ul>
 *   <li>如果 JSON 反序列化失敗，會捕獲異常並記錄錯誤</li>
 *   <li>不會中斷應用程式執行，但訊息可能無法送達客戶端</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.RedisMessageListenerTests
 * @see com.hejz.springbootstomp.RedisMessagePublisher
 * @see org.springframework.data.redis.connection.MessageListener
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Component
public class RedisMessageListener implements MessageListener {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 處理從 Redis Pub/Sub 頻道接收到的訊息
     * 
     * <p>此方法在 RedisMessageListenerContainer 收到 Redis 頻道訊息時被調用。
     * 方法會將接收到的 JSON 字串反序列化為 ResponseMessage 物件，然後透過
     * SimpMessagingTemplate 轉發給所有連接的 WebSocket 客戶端。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>從 Redis Message 物件中提取頻道名稱和訊息內容</li>
     *   <li>將訊息內容（JSON 字串）反序列化為 ResponseMessage 物件</li>
     *   <li>透過 SimpMessagingTemplate 轉發到 WebSocket /topic/chat 頻道</li>
     *   <li>所有訂閱該頻道的客戶端收到訊息</li>
     * </ol>
     * 
     * <p>錯誤處理：
     * <ul>
     *   <li>如果 JSON 反序列化失敗，會捕獲異常並記錄錯誤</li>
     *   <li>不會中斷應用程式執行，但該訊息無法送達客戶端</li>
     * </ul>
     * 
     * @param message Redis 訊息物件，包含頻道名稱和訊息內容
     * @param pattern 訊息匹配模式（目前未使用）
     * 
     * @see com.hejz.springbootstomp.RedisMessageListenerTests#testOnMessage()
     * @see com.hejz.springbootstomp.RedisMessageListenerTests#testOnMessageWithInvalidJson()
     * @see com.hejz.springbootstomp.RedisMessageListenerTests#testOnMessageForwardsToWebSocket()
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 提取 Redis 頻道名稱和訊息內容
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());
        
        log.info("=== Redis 訊息接收 ===");
        log.info("Redis 頻道: {}", channel);
        log.info("訊息內容: {}", body);
        
        // 從 Redis 頻道接收訊息，轉發到 WebSocket 客戶端
        try {
            // 將 JSON 字串反序列化為 ResponseMessage 物件
            ResponseMessage responseMessage = objectMapper.readValue(body, ResponseMessage.class);
            log.info("反序列化成功，轉發到 /topic/chat");
            // 轉發到 WebSocket 的 /topic/chat 頻道，所有訂閱的客戶端都會收到
            messagingTemplate.convertAndSend("/topic/chat", responseMessage);
            log.info("✓ 訊息已轉發到 WebSocket /topic/chat 頻道");
        } catch (Exception e) {
            // 記錄錯誤，但不中斷執行
            // 使用 logger 記錄錯誤，避免在測試中打印堆棧跟踪
            log.error("Redis 訊息處理失敗: {}", e.getMessage(), e);
        }
    }
}

