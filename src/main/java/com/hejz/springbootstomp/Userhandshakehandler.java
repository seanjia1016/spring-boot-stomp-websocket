package com.hejz.springbootstomp;

import com.sun.security.auth.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * 自訂 WebSocket 握手處理器
 * 
 * <p>此類別繼承自 {@link DefaultHandshakeHandler}，用於在 WebSocket 連接建立時
 * 為每個客戶端分配唯一的識別碼（Principal）。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>在 WebSocket 握手階段自動為每個連接生成唯一的使用者 ID</li>
 *   <li>將生成的 ID 作為 Principal 返回，用於後續的訊息路由和用戶識別</li>
 *   <li>記錄用戶登入資訊，便於追蹤和除錯</li>
 * </ul>
 * 
 * <p>使用場景：
 * <ul>
 *   <li>私信功能：使用生成的 ID 來識別訊息接收者</li>
 *   <li>用戶追蹤：記錄哪些用戶已連接</li>
 *   <li>權限控制：可基於 Principal 進行權限驗證</li>
 * </ul>
 * 
 * <p>注意事項：
 * <ul>
 *   <li>生成的 ID 為 UUID 格式，去除連字號後的 32 位字串</li>
 *   <li>每個 WebSocket 連接都會獲得一個新的唯一 ID</li>
 *   <li>在生產環境中，可考慮使用客戶端提供的 token 或用戶 ID 替代隨機生成</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.UserhandshakehandlerTests
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
public class Userhandshakehandler extends DefaultHandshakeHandler {
    
    /**
     * 決定 WebSocket 連接的使用者身份
     * 
     * <p>此方法在 WebSocket 握手階段被調用，用於為每個連接分配一個唯一的
     * 使用者識別碼。生成的 ID 將作為 Principal 返回，並在後續的訊息處理中
     * 用於識別訊息發送者和接收者。
     * 
     * <p>實作細節：
     * <ol>
     *   <li>生成一個 UUID 並移除連字號，得到 32 位字串</li>
     *   <li>將生成的 ID 記錄到日誌中</li>
     *   <li>創建 UserPrincipal 物件並返回</li>
     * </ol>
     * 
     * <p>未來擴展：
     * <ul>
     *   <li>可從 HTTP 請求頭中提取 token 或用戶 ID</li>
     *   <li>可從 attributes 中獲取預先設定的用戶資訊</li>
     *   <li>可進行用戶認證和授權檢查</li>
     * </ul>
     * 
     * @param request HTTP 請求物件，包含請求頭、參數等資訊
     * @param wsHandler WebSocket 處理器，用於處理 WebSocket 訊息
     * @param attributes 屬性映射，可在握手前預先設定用戶資訊
     * @return Principal 物件，包含唯一的使用者識別碼
     * 
     * @see com.hejz.springbootstomp.UserhandshakehandlerTests#testDetermineUser()
     * @see com.hejz.springbootstomp.UserhandshakehandlerTests#testDetermineUserGeneratesUniqueId()
     */
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes){
        // 生成唯一的使用者 ID（UUID 格式，去除連字號）
        // 在生產環境中，此 ID 可以是客戶端提供的用戶 ID 或 token 值
        final String id = UUID.randomUUID().toString().replaceAll("-","");
        log.info("登入用戶 ID: {}", id);
        return new UserPrincipal(id);
    }
}