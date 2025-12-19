package com.hejz.springbootstomp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
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
        // 建立要測試的 WebSocketConfig 實例
        webSocketConfig = new WebSocketConfig();
        // 建立模擬的端點註冊表（Mock 物件），用於模擬 Spring 的 StompEndpointRegistry
        endpointRegistry = mock(StompEndpointRegistry.class);
        // 建立模擬的訊息代理註冊表（Mock 物件），用於模擬 Spring 的 MessageBrokerRegistry
        messageBrokerRegistry = mock(MessageBrokerRegistry.class);
    }

    /**
     * 測試 STOMP 端點註冊
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：endpointRegistry（模擬的端點註冊表）</li>
     *   <li>透過方法：registerStompEndpoints(endpointRegistry)</li>
     *   <li>預期結果：應該呼叫 addEndpoint("our-websocket")、setHandshakeHandler()、withSockJS() 各 1 次</li>
     * </ul>
     * 
     * <p>實際場景：當 Spring Boot 啟動時，會呼叫這個方法來註冊 WebSocket 端點，
     * 讓客戶端可以透過 "/our-websocket" 這個路徑來建立 WebSocket 連接。
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#registerStompEndpoints(StompEndpointRegistry)
     */
    @Test
    @Order(1)
    void testRegisterStompEndpoints() {
        // 建立模擬的端點註冊物件，用於模擬 addEndpoint() 方法的返回值
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        // 建立模擬的 SockJS 服務註冊物件，用於模擬 withSockJS() 方法的返回值
        SockJsServiceRegistration sockJsRegistration = mock(SockJsServiceRegistration.class);
        
        // 設定 Mock 行為：當呼叫 addEndpoint("our-websocket") 時，返回 registration 物件
        when(endpointRegistry.addEndpoint("our-websocket")).thenReturn(registration);
        // 設定 Mock 行為：當呼叫 setHandshakeHandler() 時，接受任何 Userhandshakehandler 實例
        // 使用 any() 是因為實際程式碼中會 new Userhandshakehandler()，每次都是新實例
        // 我們無法預先知道具體的實例引用，所以用 any() 來匹配任何該類型的實例
        when(registration.setHandshakeHandler(any(Userhandshakehandler.class))).thenReturn(registration);
        // 設定 Mock 行為：當呼叫 withSockJS() 時，返回 sockJsRegistration 物件
        when(registration.withSockJS()).thenReturn(sockJsRegistration);

        // 執行被測試的方法：註冊 STOMP 端點
        webSocketConfig.registerStompEndpoints(endpointRegistry);

        // 驗證：確認 addEndpoint("our-websocket") 被呼叫了 1 次
        verify(endpointRegistry, times(1)).addEndpoint("our-websocket");
        // 驗證：確認 setHandshakeHandler() 被呼叫了 1 次，參數是任何 Userhandshakehandler 實例
        // 使用 any() 是因為實際程式碼中每次都會創建新的 Userhandshakehandler 實例
        // 我們只關心是否有傳入該類型的物件，不關心具體是哪個實例
        verify(registration, times(1)).setHandshakeHandler(any(Userhandshakehandler.class));
        // 驗證：確認 withSockJS() 被呼叫了 1 次
        verify(registration, times(1)).withSockJS();
    }

    /**
     * 測試 STOMP 端點配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：endpointRegistry（模擬的端點註冊表）</li>
     *   <li>透過方法：registerStompEndpoints(endpointRegistry)</li>
     *   <li>預期結果：端點註冊表不應為 null，表示配置成功</li>
     * </ul>
     * 
     * <p>實際場景：驗證端點註冊後，註冊表物件仍然存在且有效。
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#registerStompEndpoints(StompEndpointRegistry)
     */
    @Test
    @Order(2)
    void testStompEndpointConfiguration() {
        // 建立模擬的端點註冊物件，用於模擬 addEndpoint() 方法的返回值
        StompWebSocketEndpointRegistration registration = mock(StompWebSocketEndpointRegistration.class);
        // 建立模擬的 SockJS 服務註冊物件，用於模擬 withSockJS() 方法的返回值
        org.springframework.web.socket.config.annotation.SockJsServiceRegistration sockJsRegistration = 
            mock(org.springframework.web.socket.config.annotation.SockJsServiceRegistration.class);
        
        // 設定 Mock 行為：當呼叫 addEndpoint("our-websocket") 時，返回 registration 物件
        when(endpointRegistry.addEndpoint("our-websocket")).thenReturn(registration);
        // 設定 Mock 行為：當呼叫 setHandshakeHandler() 時，接受任何 Userhandshakehandler 實例
        // 使用 any() 是因為實際程式碼中會 new Userhandshakehandler()，每次都是新實例
        when(registration.setHandshakeHandler(any(Userhandshakehandler.class))).thenReturn(registration);
        // 設定 Mock 行為：當呼叫 withSockJS() 時，返回 sockJsRegistration 物件
        when(registration.withSockJS()).thenReturn(sockJsRegistration);

        // 執行被測試的方法：註冊 STOMP 端點
        webSocketConfig.registerStompEndpoints(endpointRegistry);

        // 驗證端點已正確配置：確認端點註冊表不為 null
        assertNotNull(endpointRegistry, "端點註冊表不應為 null");
    }

    /**
     * 測試訊息代理配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：messageBrokerRegistry（模擬的訊息代理註冊表）</li>
     *   <li>透過方法：configureMessageBroker(messageBrokerRegistry)</li>
     *   <li>預期結果：應該呼叫 enableSimpleBroker("/topic", "/user")、setApplicationDestinationPrefixes("ws")、setUserDestinationPrefix("/user") 各 1 次</li>
     * </ul>
     * 
     * <p>實際場景：當 Spring Boot 啟動時，會呼叫這個方法來設定訊息路由規則。
     * "/topic" 用於公共頻道（所有人都能收到），"/user" 用於私信頻道（只有特定使用者能收到），
     * "ws" 是客戶端發送訊息時使用的前綴。
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#configureMessageBroker(MessageBrokerRegistry)
     */
    @Test
    @Order(3)
    void testConfigureMessageBroker() {
        // 建立模擬的簡單代理註冊物件，用於模擬 enableSimpleBroker() 方法的返回值
        org.springframework.messaging.simp.config.SimpleBrokerRegistration simpleBroker = 
            mock(org.springframework.messaging.simp.config.SimpleBrokerRegistration.class);
        
        // 設定 Mock 行為：當呼叫 enableSimpleBroker("/topic", "/user") 時，返回 simpleBroker 物件
        // "/topic" 用於公共頻道，"/user" 用於用戶目標頻道
        when(messageBrokerRegistry.enableSimpleBroker("/topic", "/user")).thenReturn(simpleBroker);
        // 設定 Mock 行為：當呼叫 setApplicationDestinationPrefixes("ws") 時，返回 messageBrokerRegistry 本身
        // "ws" 是應用程式目標前綴，客戶端發送訊息時使用
        when(messageBrokerRegistry.setApplicationDestinationPrefixes("ws")).thenReturn(messageBrokerRegistry);
        // 設定 Mock 行為：當呼叫 setUserDestinationPrefix("/user") 時，返回 messageBrokerRegistry 本身
        // "/user" 是用戶目標前綴，用於私信功能
        when(messageBrokerRegistry.setUserDestinationPrefix("/user")).thenReturn(messageBrokerRegistry);

        // 執行被測試的方法：配置訊息代理
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // 驗證：確認 enableSimpleBroker("/topic", "/user") 被呼叫了 1 次
        verify(messageBrokerRegistry, times(1)).enableSimpleBroker("/topic", "/user");
        // 驗證：確認 setApplicationDestinationPrefixes("ws") 被呼叫了 1 次
        verify(messageBrokerRegistry, times(1)).setApplicationDestinationPrefixes("ws");
        // 驗證：確認 setUserDestinationPrefix("/user") 被呼叫了 1 次
        verify(messageBrokerRegistry, times(1)).setUserDestinationPrefix("/user");
    }

    /**
     * 測試訊息代理前綴配置
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：messageBrokerRegistry（模擬的訊息代理註冊表）</li>
     *   <li>透過方法：configureMessageBroker(messageBrokerRegistry)</li>
     *   <li>預期結果：所有前綴配置方法都應該被正確呼叫，確保訊息路由規則正確設定</li>
     * </ul>
     * 
     * <p>實際場景：驗證訊息代理的前綴設定是否正確，確保公共頻道和私信頻道都能正常運作。
     * 
     * @see com.hejz.springbootstomp.WebSocketConfig#configureMessageBroker(MessageBrokerRegistry)
     */
    @Test
    @Order(4)
    void testMessageBrokerPrefixes() {
        // 建立模擬的簡單代理註冊物件，用於模擬 enableSimpleBroker() 方法的返回值
        org.springframework.messaging.simp.config.SimpleBrokerRegistration simpleBroker = 
            mock(org.springframework.messaging.simp.config.SimpleBrokerRegistration.class);
        
        // 設定 Mock 行為：當呼叫 enableSimpleBroker("/topic", "/user") 時，返回 simpleBroker 物件
        // "/topic" 用於公共頻道，"/user" 用於用戶目標頻道
        when(messageBrokerRegistry.enableSimpleBroker("/topic", "/user")).thenReturn(simpleBroker);
        // 設定 Mock 行為：當呼叫 setApplicationDestinationPrefixes("ws") 時，返回 messageBrokerRegistry 本身
        // "ws" 是應用程式目標前綴，客戶端發送訊息時使用
        when(messageBrokerRegistry.setApplicationDestinationPrefixes("ws")).thenReturn(messageBrokerRegistry);
        // 設定 Mock 行為：當呼叫 setUserDestinationPrefix("/user") 時，返回 messageBrokerRegistry 本身
        // "/user" 是用戶目標前綴，用於私信功能
        when(messageBrokerRegistry.setUserDestinationPrefix("/user")).thenReturn(messageBrokerRegistry);

        // 執行被測試的方法：配置訊息代理
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // 驗證前綴已正確配置：確認 enableSimpleBroker("/topic", "/user") 被呼叫了 1 次
        verify(messageBrokerRegistry, times(1)).enableSimpleBroker("/topic", "/user");
        // 驗證前綴已正確配置：確認 setApplicationDestinationPrefixes("ws") 被呼叫了 1 次
        verify(messageBrokerRegistry, times(1)).setApplicationDestinationPrefixes("ws");
        // 驗證前綴已正確配置：確認 setUserDestinationPrefix("/user") 被呼叫了 1 次
        verify(messageBrokerRegistry, times(1)).setUserDestinationPrefix("/user");
    }
}

