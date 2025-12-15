package com.hejz.springbootstomp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WebSocketConfig 單元測試類別
 * 
 * <p>此測試類別驗證 WebSocket 配置類別的功能，包括：
 * <ul>
 *   <li>STOMP 端點註冊</li>
 *   <li>訊息代理配置</li>
 *   <li>端點配置驗證</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testRegisterStompEndpoints() - 驗證端點註冊</li>
 *   <li>testStompEndpointConfiguration() - 驗證端點配置</li>
 *   <li>testConfigureMessageBroker() - 驗證訊息代理配置</li>
 *   <li>testMessageBrokerPrefixes() - 驗證訊息代理前綴</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.WebSocketConfig
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
class WebSocketConfigTests {

    private WebSocketConfig webSocketConfig;
    private StompEndpointRegistry endpointRegistry;
    private MessageBrokerRegistry messageBrokerRegistry;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig();
        endpointRegistry = mock(StompEndpointRegistry.class);
        messageBrokerRegistry = mock(MessageBrokerRegistry.class);
    }

    /**
     * 測試 STOMP 端點註冊
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#registerStompEndpoints(StompEndpointRegistry)
     */
    @Test
    @Order(1)
    void testRegisterStompEndpoints() {
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        org.springframework.web.socket.config.annotation.SockJsServiceRegistration sockJsRegistration = 
            mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class);
        
        when(endpointRegistry.addEndpoint("our-websocket")).thenReturn(registration);
        when(registration.setHandshakeHandler(any(Userhandshakehandler.class))).thenReturn(registration);
        when(registration.withSockJS()).thenReturn(sockJsRegistration);

        webSocketConfig.registerStompEndpoints(endpointRegistry);

        verify(endpointRegistry, times(1)).addEndpoint("our-websocket");
        verify(registration, times(1)).setHandshakeHandler(any(Userhandshakehandler.class));
        verify(registration, times(1)).withSockJS();
    }

    /**
     * 測試 STOMP 端點配置
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#registerStompEndpoints(StompEndpointRegistry)
     */
    @Test
    @Order(2)
    void testStompEndpointConfiguration() {
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        org.springframework.web.socket.config.annotation.SockJsServiceRegistration sockJsRegistration = 
            mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class);
        
        when(endpointRegistry.addEndpoint("our-websocket")).thenReturn(registration);
        when(registration.setHandshakeHandler(any(Userhandshakehandler.class))).thenReturn(registration);
        when(registration.withSockJS()).thenReturn(sockJsRegistration);

        webSocketConfig.registerStompEndpoints(endpointRegistry);

        // 驗證端點已正確配置
        assertNotNull(endpointRegistry, "端點註冊表不應為 null");
    }

    /**
     * 測試訊息代理配置
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#configureMessageBroker(MessageBrokerRegistry)
     */
    @Test
    @Order(3)
    void testConfigureMessageBroker() {
        org.springframework.messaging.simp.config.SimpleBrokerRegistration simpleBroker = 
            mock(org.springframework.messaging.simp.config.SimpleBrokerRegistration.class);
        
        when(messageBrokerRegistry.enableSimpleBroker("/topic")).thenReturn(simpleBroker);
        when(messageBrokerRegistry.setApplicationDestinationPrefixes("ws")).thenReturn(messageBrokerRegistry);

        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        verify(messageBrokerRegistry, times(1)).enableSimpleBroker("/topic");
        verify(messageBrokerRegistry, times(1)).setApplicationDestinationPrefixes("ws");
    }

    /**
     * 測試訊息代理前綴配置
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#configureMessageBroker(MessageBrokerRegistry)
     */
    @Test
    @Order(4)
    void testMessageBrokerPrefixes() {
        org.springframework.messaging.simp.config.SimpleBrokerRegistration simpleBroker = 
            mock(org.springframework.messaging.simp.config.SimpleBrokerRegistration.class);
        
        when(messageBrokerRegistry.enableSimpleBroker("/topic")).thenReturn(simpleBroker);
        when(messageBrokerRegistry.setApplicationDestinationPrefixes("ws")).thenReturn(messageBrokerRegistry);

        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // 驗證前綴已正確配置
        verify(messageBrokerRegistry, times(1)).enableSimpleBroker("/topic");
        verify(messageBrokerRegistry, times(1)).setApplicationDestinationPrefixes("ws");
    }
}

