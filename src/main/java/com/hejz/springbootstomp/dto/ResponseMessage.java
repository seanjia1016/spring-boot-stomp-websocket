package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 回應訊息資料傳輸物件（DTO）
 * 
 * <p>此類別用於封裝發送給 WebSocket 客戶端的回應訊息資料。
 * 主要用於將伺服器端的訊息內容傳遞給客戶端。
 * 
 * <p>欄位說明：
 * <ul>
 *   <li>content：回應訊息內容</li>
 * </ul>
 * 
 * <p>使用場景：
 * <ul>
 *   <li>公共聊天訊息：發送給所有客戶端</li>
 *   <li>個人私信：發送給特定用戶</li>
 *   <li>系統通知：伺服器主動推送的通知訊息</li>
 * </ul>
 * 
 * <p>序列化：
 * <ul>
 *   <li>此物件會被序列化為 JSON 格式發送給客戶端</li>
 *   <li>使用 Jackson 進行 JSON 序列化和反序列化</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.dto.Message
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResponseMessage {
    /** 回應訊息內容 */
    private String content;
}
