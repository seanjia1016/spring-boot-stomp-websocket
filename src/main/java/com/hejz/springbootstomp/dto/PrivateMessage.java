package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 私信訊息資料傳輸物件（DTO）
 * 
 * <p>此類別用於封裝通過 Redis Pub/Sub 傳遞的私信訊息，包含發送者、
 * 接收者和訊息內容等資訊。
 * 
 * <p>欄位說明：
 * <ul>
 *   <li>type：訊息類型，固定為 "private"</li>
 *   <li>recipientId：接收者 ID（必填）</li>
 *   <li>senderId：發送者 ID（必填）</li>
 *   <li>senderName：發送者名稱（可選，用於顯示）</li>
 *   <li>content：訊息內容（必填）</li>
 *   <li>timestamp：訊息時間戳（毫秒，可選）</li>
 * </ul>
 * 
 * <p>使用場景：
 * <ul>
 *   <li>通過 Redis Pub/Sub 跨節點傳遞私信</li>
 *   <li>各節點監聽器接收後轉發給目標用戶</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.RedisMessageListener
 * @see com.hejz.springbootstomp.RedisMessagePublisher
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 訊息類型，固定為 "private" */
    private String type = "private";
    
    /** 接收者 ID（必填） */
    private String recipientId;
    
    /** 發送者 ID（必填） */
    private String senderId;
    
    /** 發送者名稱（可選，用於顯示） */
    private String senderName;
    
    /** 訊息內容（必填） */
    private String content;
    
    /** 訊息時間戳（毫秒，可選） */
    private Long timestamp;
}

