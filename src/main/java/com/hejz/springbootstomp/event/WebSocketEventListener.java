package com.hejz.springbootstomp.event;

import com.hejz.springbootstomp.service.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * WebSocket 事件監聽器
 * 
 * <p>此監聽器負責監聽 WebSocket 連接和斷開事件，
 * 並更新專員的線上狀態。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>監聽連接事件：當專員連接時，標記為在線並廣播狀態</li>
 *   <li>監聽斷開事件：當專員斷開時，標記為離線並廣播狀態</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private AgentStatusService agentStatusService;

    /**
     * 處理 WebSocket 連接事件
     * 
     * <p>當專員建立 WebSocket 連接時，此方法會被調用。
     * 會更新專員狀態為在線，並廣播狀態更新給對方。
     * 
     * @param event WebSocket 連接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("=== WebSocket 連接事件 ===");
        log.info("會話ID: {}", sessionId);
        log.info("訊息類型: {}", headerAccessor.getMessageType());
        log.info("命令: {}", headerAccessor.getCommand());
        log.info("所有標頭: {}", headerAccessor.toMap());
        
        // 從 Principal 獲取用戶ID
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            log.info("✓ 找到用戶 Principal，用戶ID: {}", userId);
            
            // 延遲一點時間再更新狀態，確保Redis中的ID已經設置
            // 使用異步方式，避免阻塞
            new Thread(() -> {
                try {
                    log.info("等待200毫秒後更新專員狀態，用戶ID: {}", userId);
                    Thread.sleep(200); // 增加等待時間到200毫秒
                    log.info("開始更新專員狀態為在線，用戶ID: {}", userId);
                    agentStatusService.setAgentOnline(userId);
                    log.info("專員狀態更新完成，用戶ID: {}", userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("更新專員狀態時被中斷，用戶ID: {}", userId, e);
                } catch (Exception e) {
                    log.error("更新專員狀態時發生錯誤，用戶ID: {}", userId, e);
                }
            }).start();
        } else {
            log.warn("⚠ 連接事件中沒有找到用戶 Principal，會話ID: {}", sessionId);
            log.warn("所有標頭內容: {}", headerAccessor.toMap());
        }
    }

    /**
     * 處理 WebSocket 斷開事件
     * 
     * <p>當專員斷開 WebSocket 連接時，此方法會被調用。
     * 會更新專員狀態為離線，並廣播狀態更新給對方。
     * 
     * @param event WebSocket 斷開事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("收到 WebSocket 斷開事件，會話ID: {}", sessionId);
        
        // 從 Principal 獲取用戶ID
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            log.info("用戶已斷開: {}", userId);
            
            // 更新專員狀態為離線
            agentStatusService.setAgentOffline(userId);
        } else {
            log.warn("斷開事件中沒有找到用戶 Principal，會話ID: {}", sessionId);
        }
    }

    /**
     * 處理 WebSocket 訂閱事件
     * 
     * <p>當客戶端訂閱頻道時，此方法會被調用。
     * 這是一個備用方案，確保在客戶端訂閱時也能更新狀態。
     * 
     * @param event WebSocket 訂閱事件
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        log.info("=== WebSocket 訂閱事件 ===");
        log.info("會話ID: {}", sessionId);
        log.info("訂閱頻道: {}", destination);
        
        // 從 Principal 獲取用戶ID
        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            log.info("用戶已訂閱頻道，用戶ID: {}, 頻道: {}", userId, destination);
            
            // 如果是訂閱公共頻道，可能是新連接，嘗試更新狀態
            if (destination != null && destination.startsWith("/topic/")) {
                log.info("檢測到公共頻道訂閱，嘗試更新專員狀態，用戶ID: {}", userId);
                // 延遲更新狀態，確保Redis中的ID已經設置
                new Thread(() -> {
                    try {
                        log.info("等待300毫秒後更新專員狀態（訂閱事件），用戶ID: {}", userId);
                        Thread.sleep(300);
                        log.info("開始更新專員狀態為在線（訂閱事件），用戶ID: {}", userId);
                        agentStatusService.setAgentOnline(userId);
                        log.info("專員狀態更新完成（訂閱事件），用戶ID: {}", userId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("更新專員狀態時被中斷（訂閱事件），用戶ID: {}", userId, e);
                    } catch (Exception e) {
                        log.error("更新專員狀態時發生錯誤（訂閱事件），用戶ID: {}", userId, e);
                    }
                }).start();
            }
        } else {
            log.warn("訂閱事件中沒有找到用戶 Principal，會話ID: {}", sessionId);
        }
    }
}

