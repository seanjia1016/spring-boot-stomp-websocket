package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客戶端狀態檢查訊息 DTO
 * 
 * <p>用於 RabbitMQ 延遲訊息，當延遲時間到達時觸發客戶端狀態檢查。
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientStatusCheckMessage {
    /**
     * 客戶端 ID
     */
    private String clientId;
    
    /**
     * 預期的最後心跳時間（用於比較是否超時）
     */
    private Long expectedLastHeartbeatTime;
    
    /**
     * 檢查時間戳
     */
    private Long checkTimestamp;
}



