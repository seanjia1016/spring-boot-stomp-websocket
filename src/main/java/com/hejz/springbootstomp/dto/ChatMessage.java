package com.hejz.springbootstomp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 聊天訊息 DTO
 * 
 * <p>用於持久化到 Redis 的訊息物件，包含訊息的完整資訊。
 * 
 * <p>欄位說明：
 * <ul>
 *   <li>senderId: 發送者 ID</li>
 *   <li>senderName: 發送者名稱（可選）</li>
 *   <li>content: 訊息內容</li>
 *   <li>timestamp: 訊息時間戳（毫秒）</li>
 *   <li>type: 訊息類型（public=公共訊息，private=私信）</li>
 *   <li>recipientId: 接收者 ID（僅私信時使用）</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 發送者 ID
     */
    @JsonProperty("senderId")
    private String senderId;
    
    /**
     * 發送者名稱（可選，用於顯示）
     */
    @JsonProperty("senderName")
    private String senderName;
    
    /**
     * 訊息內容
     */
    @JsonProperty("content")
    private String content;
    
    /**
     * 訊息時間戳（毫秒）
     */
    @JsonProperty("timestamp")
    private Long timestamp;
    
    /**
     * 訊息類型：public（公共訊息）或 private（私信）
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * 接收者 ID（僅私信時使用）
     */
    @JsonProperty("recipientId")
    private String recipientId;
}









