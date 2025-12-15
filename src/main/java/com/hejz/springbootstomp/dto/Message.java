package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 訊息資料傳輸物件（DTO）
 * 
 * <p>此類別用於封裝從 WebSocket 客戶端接收的訊息資料，包括訊息內容、
 * 接收者資訊等。
 * 
 * <p>欄位說明：
 * <ul>
 *   <li>content：訊息內容，必填欄位</li>
 *   <li>recipient：私信接收者 ID，用於指定私信目標用戶</li>
 *   <li>id：私信接收者 ID（兼容舊格式），優先使用此欄位</li>
 * </ul>
 * 
 * <p>使用場景：
 * <ul>
 *   <li>公共訊息：僅需設定 content 欄位</li>
 *   <li>私信訊息：需設定 content 和 id（或 recipient）欄位</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.dto.ResponseMessage
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    /** 訊息內容 */
    private String content;
    
    /** 私信接收者 ID（新格式） */
    private String recipient;
    
    /** 私信接收者 ID（舊格式，優先使用） */
    private String id;
}
