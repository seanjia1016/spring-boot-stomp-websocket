package com.hejz.springbootstomp;

import com.hejz.springbootstomp.dto.PrivateMessage;
import com.hejz.springbootstomp.dto.ResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

/**
 * Redis 訊息發布服務
 * 
 * <p>此服務類別負責將訊息發布到 Redis Pub/Sub 頻道，實現多節點部署時的
 * 訊息同步。當訊息發布到 Redis 後，所有節點的 RedisMessageListener 都會
 * 收到訊息並轉發給各自的 WebSocket 客戶端。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>將訊息序列化為 JSON 格式</li>
 *   <li>發布訊息到 Redis /topic/chat 頻道</li>
 *   <li>支援 ResponseMessage 物件和字串兩種格式的訊息</li>
 * </ul>
 * 
 * <p>訊息流程：
 * <ol>
 *   <li>接收訊息（ResponseMessage 物件或字串）</li>
 *   <li>將訊息序列化為 JSON 字串</li>
 *   <li>發布到 Redis /topic/chat 頻道</li>
 *   <li>Redis 監聽器接收並轉發給所有節點的客戶端</li>
 * </ol>
 * 
 * <p>錯誤處理：
 * <ul>
 *   <li>如果 JSON 序列化失敗，會捕獲異常並記錄錯誤</li>
 *   <li>不會中斷應用程式執行，但訊息可能無法送達</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.RedisMessagePublisherTests
 * @see com.hejz.springbootstomp.RedisMessageListener
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Service
public class RedisMessagePublisher {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** Redis Pub/Sub 頻道主題：/topic/chat */
    private final ChannelTopic topic = new ChannelTopic("/topic/chat");
    
    /** Redis Pub/Sub 頻道主題：/topic/privateMessage */
    private final ChannelTopic privateTopic = new ChannelTopic("/topic/privateMessage");

    /**
     * 發布訊息到 Redis 頻道（ResponseMessage 物件格式）
     * 
     * <p>此方法接收 ResponseMessage 物件，將其序列化為 JSON 字串後發布到
     * Redis Pub/Sub 頻道。所有訂閱該頻道的節點都會收到此訊息。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>使用 ObjectMapper 將 ResponseMessage 序列化為 JSON 字串</li>
     *   <li>透過 RedisTemplate 發布到 /topic/chat 頻道</li>
     *   <li>Redis 監聽器自動將訊息轉發給所有訂閱的節點</li>
     * </ol>
     * 
     * <p>錯誤處理：
     * <ul>
     *   <li>如果 JSON 序列化失敗，會捕獲 JsonProcessingException</li>
     *   <li>錯誤會記錄到標準錯誤輸出，但不會拋出異常</li>
     * </ul>
     * 
     * @param message 要發布的 ResponseMessage 物件
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisherTests#testPublishWithResponseMessage()
     * @see com.hejz.springbootstomp.RedisMessagePublisherTests#testPublishWithNullMessage()
     * @see com.hejz.springbootstomp.RedisMessagePublisherTests#testPublishHandlesJsonException()
     */
    public void publish(ResponseMessage message) {
        try {
            // 將 ResponseMessage 序列化為 JSON 字串
            String jsonMessage = objectMapper.writeValueAsString(message);
            // 發布到 Redis /topic/chat 頻道
            redisTemplate.convertAndSend(topic.getTopic(), jsonMessage);
        } catch (JsonProcessingException e) {
            // 記錄錯誤，但不中斷執行
            // 使用 logger 記錄錯誤，避免在測試中打印堆棧跟踪
            log.error("Redis 訊息發布失敗: {}", e.getMessage());
        }
    }

    /**
     * 發布訊息到 Redis 頻道（字串格式）
     * 
     * <p>此方法接收字串格式的訊息，將其包裝為 ResponseMessage 物件後
     * 調用 publish(ResponseMessage) 方法發布到 Redis。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>將字串訊息包裝為 ResponseMessage 物件</li>
     *   <li>調用 publish(ResponseMessage) 方法發布訊息</li>
     * </ol>
     * 
     * <p>使用場景：
     * <ul>
     *   <li>簡化 API：當只需要發送簡單文字訊息時，不需要創建 ResponseMessage 物件</li>
     *   <li>向後兼容：支援舊版 API 的字串參數格式</li>
     * </ul>
     * 
     * @param message 要發布的訊息內容（字串格式）
     * 
     * @see com.hejz.springbootstomp.RedisMessagePublisherTests#testPublishWithString()
     * @see com.hejz.springbootstomp.RedisMessagePublisherTests#testPublishWithEmptyString()
     * @see #publish(ResponseMessage)
     */
    public void publish(String message) {
        // 將字串訊息包裝為 ResponseMessage 物件
        ResponseMessage responseMessage = new ResponseMessage(message);
        // 調用 publish(ResponseMessage) 方法發布訊息
        publish(responseMessage);
    }

    /**
     * 發布私信訊息到 Redis 頻道
     * 
     * <p>此方法接收 PrivateMessage 物件，將其序列化為 JSON 字串後發布到
     * Redis Pub/Sub 頻道。所有訂閱該頻道的節點都會收到此訊息，但只有
     * 目標用戶連接的節點才會轉發給客戶端。
     * 
     * <p>處理流程：
     * <ol>
     *   <li>使用 ObjectMapper 將 PrivateMessage 序列化為 JSON 字串</li>
     *   <li>透過 RedisTemplate 發布到 /topic/privateMessage 頻道</li>
     *   <li>Redis 監聽器接收後檢查目標用戶是否連接在本節點</li>
     *   <li>如果連接，則轉發給目標用戶；否則忽略</li>
     * </ol>
     * 
     * <p>多節點支援：
     * <ul>
     *   <li>所有節點都訂閱 /topic/privateMessage 頻道</li>
     *   <li>每個節點檢查目標用戶是否連接在本節點</li>
     *   <li>只有目標用戶連接的節點才會轉發訊息</li>
     * </ul>
     * 
     * <p>錯誤處理：
     * <ul>
     *   <li>如果 JSON 序列化失敗，會捕獲 JsonProcessingException</li>
     *   <li>錯誤會記錄到日誌，但不會拋出異常</li>
     * </ul>
     * 
     * @param privateMessage 要發布的 PrivateMessage 物件，包含發送者、接收者和訊息內容
     * 
     * @see com.hejz.springbootstomp.RedisMessageListener#onMessage(Message, byte[])
     */
    public void publishPrivateMessage(PrivateMessage privateMessage) {
        try {
            // 將 PrivateMessage 序列化為 JSON 字串
            String jsonMessage = objectMapper.writeValueAsString(privateMessage);
            // 發布到 Redis /topic/privateMessage 頻道
            redisTemplate.convertAndSend(privateTopic.getTopic(), jsonMessage);
            log.info("私信已發布到 Redis /topic/privateMessage 頻道，接收者: {}", privateMessage.getRecipientId());
        } catch (JsonProcessingException e) {
            // 記錄錯誤，但不中斷執行
            log.error("Redis 私信發布失敗: {}", e.getMessage(), e);
        }
    }
}

