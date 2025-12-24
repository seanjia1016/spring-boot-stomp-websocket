package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 專員ID變更訊息 DTO
 * 
 * <p>用於 RabbitMQ 傳遞專員ID變更通知。
 * 當專員ID被覆蓋時，會發送此訊息到 RabbitMQ，
 * 通知系統處理舊連接的斷開。
 * 
 * <p>欄位說明：
 * <ul>
 *   <li>agentType：專員類型（"a" 或 "b"）</li>
 *   <li>oldId：舊的專員ID（被覆蓋的ID）</li>
 *   <li>newId：新的專員ID（當前有效的ID）</li>
 *   <li>agentName：專員名稱（"專員A" 或 "專員B"）</li>
 *   <li>timestamp：變更時間戳（毫秒）</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentIdChangeMessage {
    /** 專員類型（"a" 或 "b"） */
    private String agentType;
    
    /** 舊的專員ID（被覆蓋的ID） */
    private String oldId;
    
    /** 新的專員ID（當前有效的ID） */
    private String newId;
    
    /** 專員名稱（"專員A" 或 "專員B"） */
    private String agentName;
    
    /** 變更時間戳（毫秒） */
    private Long timestamp;
}

