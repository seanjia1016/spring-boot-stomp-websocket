package com.hejz.springbootstomp;

import com.hejz.springbootstomp.service.AgentService;
import com.sun.security.auth.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    
    private AgentService agentService;
    
    public void setAgentService(AgentService agentService) {
        this.agentService = agentService;
    }
    
    /**
     * 決定 WebSocket 連接的使用者身份
     * 
     * <p>此方法在 WebSocket 握手階段被調用，用於為每個連接分配專員ID。
     * 根據請求路徑判斷是專員A還是專員B，並從Redis獲取或創建對應的ID。
     * 
     * <p>實作細節：
     * <ol>
     *   <li>從HTTP請求URI中判斷是專員A還是專員B</li>
     *   <li>從Redis獲取對應的專員ID，如果不存在則創建新的</li>
     *   <li>使用Lua腳本確保原子性操作，如果ID改變則覆蓋舊的</li>
     *   <li>將專員ID作為 Principal 返回</li>
     * </ol>
     * 
     * @param request HTTP 請求物件，包含請求頭、參數等資訊
     * @param wsHandler WebSocket 處理器，用於處理 WebSocket 訊息
     * @param attributes 屬性映射，可在握手前預先設定用戶資訊
     * @return Principal 物件，包含專員ID
     */
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes){
        String uri = request.getURI().toString();
        String agentId;
        String agentName;
        
        // 嘗試從多個來源判斷專員類型：
        // 1. URI中包含agent-a或agent-b（包括查詢參數）
        // 2. HTTP Referer頭中包含agent-a.html或agent-b.html
        // 3. attributes中預先設置的agentType
        String referer = request.getHeaders().getFirst("Referer");
        String query = request.getURI().getQuery();
        
        // 檢查查詢參數中是否有agentType
        boolean hasAgentTypeA = query != null && (query.contains("agentType=a") || query.contains("agent=a"));
        boolean hasAgentTypeB = query != null && (query.contains("agentType=b") || query.contains("agent=b"));
        
        boolean isAgentA = uri.contains("/agent-a") || 
                          (referer != null && referer.contains("/agent-a.html")) ||
                          hasAgentTypeA ||
                          (attributes.containsKey("agentType") && "a".equals(attributes.get("agentType")));
        boolean isAgentB = uri.contains("/agent-b") || 
                          (referer != null && referer.contains("/agent-b.html")) ||
                          hasAgentTypeB ||
                          (attributes.containsKey("agentType") && "b".equals(attributes.get("agentType")));
        
        if (isAgentA) {
            // 專員A
            try {
                agentId = agentService.getOrCreateAgentAId();
                agentName = "專員A";
            } catch (Exception e) {
                log.error("獲取專員A ID失敗: {}", e.getMessage(), e);
                // Redis連接失敗時，不允許連接，拋出異常
                throw new RuntimeException("無法獲取專員A ID，Redis連接失敗。請檢查Redis服務是否正常運行。", e);
            }
        } else if (isAgentB) {
            // 專員B
            try {
                agentId = agentService.getOrCreateAgentBId();
                agentName = "專員B";
            } catch (Exception e) {
                log.error("獲取專員B ID失敗: {}", e.getMessage(), e);
                // Redis連接失敗時，不允許連接，拋出異常
                throw new RuntimeException("無法獲取專員B ID，Redis連接失敗。請檢查Redis服務是否正常運行。", e);
            }
        } else {
            // 無法判斷，使用隨機ID（向後兼容）
            agentId = UUID.randomUUID().toString().replaceAll("-", "");
            agentName = "未知用戶";
            log.warn("無法判斷專員類型，使用隨機ID: {} (URI: {}, Referer: {}, Query: {})", agentId, uri, referer, query);
        }
        
        log.info("{} 連接，ID: {}", agentName, agentId);
        
        // 將專員類型存儲到attributes中，供事件監聽器使用
        if (agentName.equals("專員A") || agentName.equals("專員B")) {
            String agentType = agentName.equals("專員A") ? "a" : "b";
            attributes.put("agentType", agentType);
            attributes.put("agentName", agentName);
        }
        
        // 將專員ID寫入臨時文件，供測試腳本讀取
        try {
            Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), "websocket_user_ids.json");
            String jsonLine = String.format("{\"userId\":\"%s\",\"agentName\":\"%s\",\"timestamp\":%d}\n", 
                    agentId, agentName, System.currentTimeMillis());
            Files.write(tempFile, jsonLine.getBytes(), java.nio.file.StandardOpenOption.CREATE, 
                       java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("無法寫入專員ID到臨時文件: {}", e.getMessage());
        }
        
        return new UserPrincipal(agentId);
    }
}