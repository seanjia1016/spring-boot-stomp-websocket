package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 心跳訊息 DTO
 * 
 * <p>用於客戶端向伺服器發送心跳請求，表示客戶端仍在線。
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatMessage {
    /**
     * 客戶端 ID（由伺服器分配的唯一識別碼）
     */
    private String clientId;
    
    /**
     * 時間戳（可選，用於記錄心跳發送時間）
     */
    private Long timestamp;
}



