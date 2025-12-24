package com.hejz.springbootstomp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 專員狀態訊息DTO
 * 
 * <p>用於在 WebSocket 中傳輸專員狀態更新訊息。
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentStatusMessage {
    /**
     * 專員類型（"a" 或 "b"）
     */
    private String agentType;
    
    /**
     * 專員名稱（"專員A" 或 "專員B"）
     */
    private String agentName;
    
    /**
     * 狀態（"ONLINE" 或 "OFFLINE"）
     */
    private String status;
    
    /**
     * 時間戳（毫秒）
     */
    private long timestamp = System.currentTimeMillis();
}










