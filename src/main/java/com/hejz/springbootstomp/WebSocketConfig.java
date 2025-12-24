package com.hejz.springbootstomp;

import com.hejz.springbootstomp.config.WebSocketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置類別
 * 
 * <p>此配置類別負責配置 Spring WebSocket 和 STOMP 協議的相關設定，
 * 包括端點註冊、訊息代理配置等。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>註冊 STOMP 端點：定義客戶端連接的 WebSocket 端點</li>
 *   <li>配置訊息代理：設定訊息路由和頻道前綴</li>
 *   <li>自訂握手處理：為每個連接分配唯一的使用者 ID</li>
 * </ul>
 * 
 * <p>配置說明：
 * <ul>
 *   <li>WebSocket 端點：/our-websocket（支援 SockJS）</li>
 *   <li>訊息代理前綴：/topic（用於訂閱頻道）</li>
 *   <li>應用程式目標前綴：ws（用於發送訊息）</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.WebSocketConfigTests
 * @see com.hejz.springbootstomp.Userhandshakehandler
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Autowired
    private WebSocketInterceptor webSocketInterceptor;
    
    @Autowired
    private com.hejz.springbootstomp.service.AgentService agentService;
    
    /**
     * 註冊 STOMP 端點
     * 
     * <p>此方法註冊 WebSocket 端點，客戶端可以透過此端點建立 WebSocket 連接。
     * 配置了自訂的握手處理器，為每個連接分配唯一的使用者 ID。
     * 
     * <p>配置說明：
     * <ul>
     *   <li>端點路徑：/our-websocket</li>
     *   <li>握手處理器：Userhandshakehandler（為每個連接生成唯一 ID）</li>
     *   <li>SockJS 支援：啟用 SockJS，提供更好的瀏覽器兼容性</li>
     * </ul>
     * 
     * <p>客戶端連接範例：
     * <pre>
     * const socket = new SockJS('/our-websocket');
     * const stompClient = Stomp.over(socket);
     * </pre>
     * 
     * @param registry STOMP 端點註冊表，用於註冊 WebSocket 端點
     * 
     * @see com.hejz.springbootstomp.WebSocketConfigTests#testRegisterStompEndpoints()
     * @see com.hejz.springbootstomp.WebSocketConfigTests#testStompEndpointConfiguration()
     */
    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        // 註冊 WebSocket 端點
        // 創建握手處理器並注入 AgentService
        Userhandshakehandler handshakeHandler = new Userhandshakehandler();
        handshakeHandler.setAgentService(agentService);
        
        registry.addEndpoint("our-websocket")
                // 添加自訂握手處理器，為每個連接分配唯一的使用者 ID
                .setHandshakeHandler(handshakeHandler)
                // 啟用 SockJS 支援，提供更好的瀏覽器兼容性
                .withSockJS();
    }

    /**
     * 配置訊息代理
     * 
     * <p>此方法配置 STOMP 訊息代理，定義訊息路由規則和頻道前綴。
     * 使用簡單的記憶體代理，適合單機或小規模部署。
     * 
     * <p>配置說明：
     * <ul>
     *   <li>訊息代理前綴：/topic（用於訂閱頻道，如 /topic/chat）</li>
     *   <li>應用程式目標前綴：ws（用於發送訊息，如 ws/message）</li>
     * </ul>
     * 
     * <p>訊息路由範例：
     * <ul>
     *   <li>客戶端訂閱：stompClient.subscribe('/topic/chat', callback)</li>
     *   <li>客戶端發送：stompClient.send('ws/message', {}, JSON.stringify(data))</li>
     *   <li>伺服器發送：messagingTemplate.convertAndSend('/topic/chat', message)</li>
     * </ul>
     * 
     * <p>注意事項：
     * <ul>
     *   <li>簡單代理使用記憶體儲存，不支援多節點部署</li>
     *   <li>多節點部署需要使用 Redis 或其他外部訊息代理</li>
     *   <li>本專案已整合 Redis Pub/Sub 實現多節點支援</li>
     * </ul>
     * 
     * @param registry 訊息代理註冊表，用於配置訊息路由規則
     * 
     * @see com.hejz.springbootstomp.WebSocketConfigTests#testConfigureMessageBroker()
     * @see com.hejz.springbootstomp.WebSocketConfigTests#testMessageBrokerPrefixes()
     */
    @Override
    public void configureMessageBroker(final MessageBrokerRegistry registry) {
        // 啟用簡單代理，支援 /topic 前綴的頻道
        // /topic 用於公共頻道（如 /topic/chat）
        // 注意：/user 前綴應該由 UserDestinationMessageHandler 處理，
        //       不應該同時在 enableSimpleBroker 中啟用，這會導致路由衝突
        registry.enableSimpleBroker("/topic");
        // 設定應用程式目標前綴，客戶端發送訊息時使用（如 /ws/message）
        // 注意：必須包含前導斜線，否則路由匹配會失敗
        registry.setApplicationDestinationPrefixes("/ws");
        // 設定用戶目標前綴，用於私信功能（convertAndSendToUser 會自動加上此前綴）
        // 當使用 convertAndSendToUser(userId, "/topic/privateMessage", message) 時
        // 實際發送的路徑會是 /user/{userId}/topic/privateMessage
        // UserDestinationMessageHandler 會將此路徑轉換為實際的訂閱路徑
        registry.setUserDestinationPrefix("/user");
    }
    
    /**
     * 配置客戶端入站通道攔截器
     * 用於記錄所有從客戶端發送到伺服器的訊息
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketInterceptor);
    }
}
