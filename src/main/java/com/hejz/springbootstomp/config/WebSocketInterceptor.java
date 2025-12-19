package com.hejz.springbootstomp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

/**
 * WebSocket 訊息攔截器
 * 用於記錄所有通過 WebSocket 傳輸的訊息，方便調試
 */
@Slf4j
@org.springframework.stereotype.Component
public class WebSocketInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            
            if (command != null) {
                String channelStr = channel != null ? channel.toString() : "null";
                boolean isOutboundChannel = channelStr.contains("clientOutboundChannel");
                boolean isBrokerChannel = channelStr.contains("brokerChannel");
                
                // 針對出站訊息（伺服器發送給客戶端）進行特別記錄
                if (isOutboundChannel && command == StompCommand.MESSAGE) {
                    log.error("=== 出站訊息攔截（preSend）===");
                    log.error("STOMP 命令: {}", command);
                    log.error("目標路徑: {}", accessor.getDestination());
                    log.error("會話 ID: {}", accessor.getSessionId());
                    log.error("用戶: {}", accessor.getUser() != null ? accessor.getUser().getName() : "null");
                    log.error("訊息通道: {}", channelStr);
                    
                    String destination = accessor.getDestination();
                    if (destination != null && destination.contains("privateMessage")) {
                        log.error(">>> 這是私信訊息！目標: {}", destination);
                        Object payload = message.getPayload();
                        if (payload instanceof byte[]) {
                            try {
                                String payloadStr = new String((byte[]) payload, "UTF-8");
                                log.error("私信內容: {}", payloadStr);
                            } catch (Exception e) {
                                log.error("無法解析私信內容: {}", e.getMessage());
                            }
                        } else {
                            log.error("私信內容: {}", payload);
                        }
                    }
                }
                
                log.info("=== WebSocket 訊息攔截 ===");
                log.info("STOMP 命令: {}", command);
                log.info("目標路徑: {}", accessor.getDestination());
                log.info("會話 ID: {}", accessor.getSessionId());
                log.info("用戶: {}", accessor.getUser() != null ? accessor.getUser().getName() : "null");
                log.info("訊息通道: {}", channel != null ? channel.getClass().getName() : "null");
                
                // 檢查訊息類型
                Object messageType = accessor.getHeader("simpMessageType");
                log.info("訊息類型 (simpMessageType): {}", messageType);
                
                if (command == StompCommand.SEND) {
                    Object payload = message.getPayload();
                    log.info("訊息內容類型: {}", payload != null ? payload.getClass().getName() : "null");
                    log.info("訊息內容: {}", payload);
                    log.info("訊息頭: {}", accessor.toMap());
                    
                    // 如果是字節數組，嘗試轉換為字串
                    if (payload instanceof byte[]) {
                        try {
                            String payloadStr = new String((byte[]) payload, "UTF-8");
                            log.info("訊息內容（字串）: {}", payloadStr);
                        } catch (Exception e) {
                            log.warn("無法將訊息內容轉換為字串: {}", e.getMessage());
                        }
                    }
                    
                    // 記錄目標路徑，確認路由配置
                    String destination = accessor.getDestination();
                    log.info("目標路徑（用於路由）: {}", destination);
                    if (destination != null && destination.startsWith("/ws/")) {
                        String mappingPath = destination.substring(4); // 移除 "/ws/" 前綴（4個字符）
                        log.info("預期的 @MessageMapping 路徑: /{}", mappingPath);
                        log.info("應該匹配 @MessageMapping(\"/{}\")", mappingPath);
                    }
                    
                    // 記錄訊息通道類型，確認是否正確進入 clientInboundChannel
                    if (channel != null) {
                        String channelName = channel.getClass().getName();
                        log.info("訊息通道類型: {}", channelName);
                        log.info("訊息通道字符串: {}", channelStr);
                        // clientInboundChannel 是 ExecutorSubscribableChannel 的實例
                        // 檢查通道字符串是否包含 "clientInboundChannel"
                        if (channelStr.contains("clientInboundChannel")) {
                            log.info("✓ 訊息已進入 clientInboundChannel");
                        } else {
                            log.warn("⚠ 訊息通道可能不是 clientInboundChannel: {}", channelStr);
                        }
                    }
                    
                    // 記錄完整的訊息頭，用於調試
                    log.info("完整訊息頭映射: {}", accessor.toMap());
                }
            }
        } else {
            log.warn("無法獲取 StompHeaderAccessor，訊息可能不是 STOMP 訊息");
        }
        
        // 確保返回原始訊息，不修改
        return message;
    }
    
    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        // 記錄訊息發送完成後的狀態
        if (ex != null) {
            log.error("訊息發送失敗: {}", ex.getMessage(), ex);
        }
        
        // 檢查是否是發送到 brokerChannel 或 clientOutboundChannel 的訊息
        if (channel != null) {
            String channelStr = channel.toString();
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                Object messageType = accessor.getHeader("simpMessageType");
                String destination = accessor.getDestination();
                StompCommand command = accessor.getCommand();
                
                if (messageType != null && messageType.toString().equals("MESSAGE")) {
                    if (channelStr.contains("brokerChannel")) {
                        log.info("✓ 訊息已發送到 brokerChannel，目標: {}", destination);
                    } else if (channelStr.contains("clientOutboundChannel")) {
                        log.error("=== 出站訊息（MESSAGE）afterSendCompletion ===");
                        log.error("✓✓✓ 私信已成功發送到 clientOutboundChannel！目標: {}", destination);
                        log.error("發送狀態: {}", sent ? "成功" : "失敗");
                        if (destination != null && destination.contains("privateMessage")) {
                            log.error(">>> 這是私信訊息！應該已經送達客戶端");
                            Object payload = message.getPayload();
                            if (payload instanceof byte[]) {
                                try {
                                    String payloadStr = new String((byte[]) payload, "UTF-8");
                                    log.error("私信內容: {}", payloadStr);
                                } catch (Exception e) {
                                    log.error("無法解析私信內容: {}", e.getMessage());
                                }
                            } else {
                                log.error("私信內容: {}", payload);
                            }
                        }
                    } else if (channelStr.contains("clientInboundChannel")) {
                        log.warn("⚠ 訊息在 clientInboundChannel 中處理，這可能不是預期的行為，目標: {}", destination);
                    }
                }
            }
        }
    }
    
    @Override
    public Message<?> postReceive(Message<?> message, MessageChannel channel) {
        // 記錄從通道接收的訊息（用於追蹤 brokerChannel 中的訊息）
        if (channel != null) {
            String channelStr = channel.toString();
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor != null) {
                Object messageType = accessor.getHeader("simpMessageType");
                String destination = accessor.getDestination();
                
                if (messageType != null && messageType.toString().equals("MESSAGE")) {
                    if (channelStr.contains("brokerChannel")) {
                        log.info("✓ 訊息從 brokerChannel 接收，目標: {}", destination);
                    } else if (channelStr.contains("clientOutboundChannel")) {
                        log.info("✓ 訊息從 clientOutboundChannel 接收，目標: {}", destination);
                    }
                }
            }
        }
        return message;
    }
}
