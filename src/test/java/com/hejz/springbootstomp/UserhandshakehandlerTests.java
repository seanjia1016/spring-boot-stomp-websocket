package com.hejz.springbootstomp;

import com.sun.security.auth.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Userhandshakehandler 單元測試類別
 * 
 * <p>此測試類別驗證自訂 WebSocket 握手處理器的功能，包括：
 * <ul>
 *   <li>使用者 ID 生成</li>
 *   <li>唯一性驗證</li>
 *   <li>Principal 創建</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testDetermineUser() - 驗證基本功能</li>
 *   <li>testDetermineUserGeneratesUniqueId() - 驗證 ID 唯一性</li>
 *   <li>testDetermineUserReturnsUserPrincipal() - 驗證 Principal 類型</li>
 *   <li>testDetermineUserIdFormat() - 驗證 ID 格式</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.Userhandshakehandler
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
class UserhandshakehandlerTests {

    private Userhandshakehandler handler;
    private ServerHttpRequest request;
    private WebSocketHandler wsHandler;
    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        handler = new Userhandshakehandler();
        request = mock(ServerHttpRequest.class);
        wsHandler = mock(WebSocketHandler.class);
        attributes = new HashMap<>();
    }

    /**
     * 測試 determineUser 方法基本功能
     * 
     * <p>此測試驗證 determineUser 方法能夠正常執行並返回 Principal 物件。
     * 
     * @see com.hejz.springbootstomp.Userhandshakehandler#determineUser(ServerHttpRequest, WebSocketHandler, Map)
     */
    @Test
    @Order(1)
    void testDetermineUser() {
        Principal principal = handler.determineUser(request, wsHandler, attributes);
        
        assertNotNull(principal, "Principal 不應為 null");
        assertNotNull(principal.getName(), "Principal 名稱不應為 null");
        assertFalse(principal.getName().isEmpty(), "Principal 名稱不應為空");
    }

    /**
     * 測試 determineUser 方法生成唯一 ID
     * 
     * <p>此測試驗證每次調用 determineUser 方法都會生成不同的使用者 ID。
     * 
     * @see com.hejz.springbootstomp.Userhandshakehandler#determineUser(ServerHttpRequest, WebSocketHandler, Map)
     */
    @Test
    @Order(2)
    void testDetermineUserGeneratesUniqueId() {
        Set<String> ids = new HashSet<>();
        
        // 生成 100 個 ID，驗證唯一性
        for (int i = 0; i < 100; i++) {
            Principal principal = handler.determineUser(request, wsHandler, attributes);
            String id = principal.getName();
            assertFalse(ids.contains(id), "ID 應唯一: " + id);
            ids.add(id);
        }
        
        assertEquals(100, ids.size(), "應生成 100 個不同的 ID");
    }

    /**
     * 測試 determineUser 返回 UserPrincipal 類型
     * 
     * <p>此測試驗證返回的 Principal 是 UserPrincipal 類型。
     * 
     * @see com.hejz.springbootstomp.Userhandshakehandler#determineUser(ServerHttpRequest, WebSocketHandler, Map)
     */
    @Test
    @Order(3)
    void testDetermineUserReturnsUserPrincipal() {
        Principal principal = handler.determineUser(request, wsHandler, attributes);
        
        assertInstanceOf(UserPrincipal.class, principal, "應返回 UserPrincipal 類型");
    }

    /**
     * 測試 determineUser 生成的 ID 格式
     * 
     * <p>此測試驗證生成的 ID 符合預期格式（32 位字串，無連字號）。
     * 
     * @see com.hejz.springbootstomp.Userhandshakehandler#determineUser(ServerHttpRequest, WebSocketHandler, Map)
     */
    @Test
    @Order(4)
    void testDetermineUserIdFormat() {
        Principal principal = handler.determineUser(request, wsHandler, attributes);
        String id = principal.getName();
        
        // UUID 去除連字號後應為 32 位字串
        assertEquals(32, id.length(), "ID 長度應為 32");
        assertTrue(id.matches("[0-9a-f]{32}"), "ID 應為 32 位十六進位字串");
        assertFalse(id.contains("-"), "ID 不應包含連字號");
    }
}

