package com.hejz.springbootstomp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot STOMP WebSocket 應用程式主類別
 * 
 * <p>此應用程式提供基於 STOMP 協議的 WebSocket 通訊功能，支援：
 * <ul>
 *   <li>公共聊天室訊息廣播</li>
 *   <li>個人私信功能</li>
 *   <li>Redis Pub/Sub 多節點訊息同步</li>
 *   <li>WebSocket 連接管理與用戶識別</li>
 * </ul>
 * 
 * <p>應用程式啟動後，可透過以下端點進行連接：
 * <ul>
 *   <li>WebSocket 端點：/our-websocket</li>
 *   <li>公共訊息頻道：/topic/chat</li>
 *   <li>私信頻道：/topic/privateMessage</li>
 * </ul>
 * 
 * @see com.hejz.springbootstomp.SpringBootStompApplicationTests#testApplicationContextLoads()
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@SpringBootApplication
public class SpringBootStompApplication {

    /**
     * 應用程式主入口方法
     * 
     * <p>此方法負責啟動 Spring Boot 應用程式，初始化所有必要的組件，
     * 包括 WebSocket 配置、Redis 連接、訊息監聽器等。
     * 
     * <p>啟動流程：
     * <ol>
     *   <li>載入 Spring Boot 應用程式上下文</li>
     *   <li>初始化 WebSocket 配置（WebSocketConfig）</li>
     *   <li>建立 Redis 連接並配置訊息監聽器</li>
     *   <li>啟動內嵌的 Tomcat 伺服器</li>
     * </ol>
     * 
     * @param args 命令列參數，可用於配置應用程式行為
     *             例如：--server.port=8080 指定伺服器端口
     * 
     * @see com.hejz.springbootstomp.SpringBootStompApplicationTests#testMainMethod()
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootStompApplication.class, args);
    }

}
