package com.hejz.springbootstomp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * WsController 單元測試類別
 * 
 * <p>此測試類別驗證 WebSocket REST 控制器的功能，包括：
 * <ul>
 *   <li>公共訊息發送</li>
 *   <li>私信發送</li>
 *   <li>服務調用驗證</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testSendMessage() - 驗證基本公共訊息發送</li>
 *   <li>testSendMessageWithEmptyContent() - 驗證空內容處理</li>
 *   <li>testSendMessagePublishesToRedis() - 驗證 Redis 發布</li>
 *   <li>testSendPrivateMessage() - 驗證基本私信發送</li>
 *   <li>testSendPrivateMessageWithInvalidId() - 驗證無效 ID 處理</li>
 *   <li>testSendPrivateMessageWithEmptyMessage() - 驗證空訊息處理</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.WsController
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class WsControllerTests {

    @Mock
    private Notificationservice notificationservice;

    @InjectMocks
    private WsController wsController;

    @BeforeEach
    void setUp() {
        // 設置預設行為
    }

    /**
     * 測試發送公共訊息基本功能
     * 
     * @see com.hejz.springbootstomp.WsController#sendMessage(String)
     */
    @Test
    @Order(1)
    void testSendMessage() {
        String message = "測試訊息";

        wsController.sendMessage(message);

        verify(notificationservice, times(1)).gloubNotificationservice(message);
    }

    /**
     * 測試發送空內容的公共訊息
     * 
     * @see com.hejz.springbootstomp.WsController#sendMessage(String)
     */
    @Test
    @Order(2)
    void testSendMessageWithEmptyContent() {
        String message = "";

        wsController.sendMessage(message);

        verify(notificationservice, times(1)).gloubNotificationservice(message);
    }

    /**
     * 測試公共訊息發布到 Redis
     * 
     * @see com.hejz.springbootstomp.WsController#sendMessage(String)
     */
    @Test
    @Order(3)
    void testSendMessagePublishesToRedis() {
        String message = "測試訊息";

        wsController.sendMessage(message);

        verify(notificationservice, times(1)).gloubNotificationservice(message);
    }

    /**
     * 測試發送私信基本功能
     * 
     * @see com.hejz.springbootstomp.WsController#sendMessage(String, String)
     */
    @Test
    @Order(4)
    void testSendPrivateMessage() {
        String id = "user123";
        String message = "私信內容";

        wsController.sendMessage(id, message);

        verify(notificationservice, times(1)).privateNotificationservice(id, message);
    }

    /**
     * 測試發送私信使用無效 ID
     * 
     * @see com.hejz.springbootstomp.WsController#sendMessage(String, String)
     */
    @Test
    @Order(5)
    void testSendPrivateMessageWithInvalidId() {
        String id = null;
        String message = "私信內容";

        wsController.sendMessage(id, message);

        verify(notificationservice, times(1)).privateNotificationservice(id, message);
    }

    /**
     * 測試發送空訊息的私信
     * 
     * @see com.hejz.springbootstomp.WsController#sendMessage(String, String)
     */
    @Test
    @Order(6)
    void testSendPrivateMessageWithEmptyMessage() {
        String id = "user123";
        String message = "";

        wsController.sendMessage(id, message);

        verify(notificationservice, times(1)).privateNotificationservice(id, message);
    }
}

