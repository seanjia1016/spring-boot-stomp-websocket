package com.hejz.springbootstomp;

import com.hejz.springbootstomp.dto.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * WsService 單元測試類別
 * 
 * <p>此測試類別驗證 WebSocket 訊息服務的功能，包括：
 * <ul>
 *   <li>公共訊息通知</li>
 *   <li>個人私信發送</li>
 *   <li>Redis 發布</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testNotify() - 驗證基本公共訊息通知</li>
 *   <li>testNotifyPublishesToRedis() - 驗證 Redis 發布</li>
 *   <li>testNotifyWithNullMessage() - 驗證 null 訊息處理</li>
 *   <li>testPrivateNotify() - 驗證基本私信發送</li>
 *   <li>testPrivateNotifyWithInvalidId() - 驗證無效 ID 處理</li>
 *   <li>testPrivateNotifyWithNullMessage() - 驗證 null 訊息處理</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.WsService
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class WsServiceTests {

    @Mock
    private SimpMessagingTemplate template;

    @Mock
    private RedisMessagePublisher redisPublisher;

    @InjectMocks
    private WsService wsService;

    @BeforeEach
    void setUp() {
        // 設置預設行為
    }

    /**
     * 測試公共訊息通知基本功能
     * 
     * @see com.hejz.springbootstomp.WsService#notify(String)
     */
    @Test
    @Order(1)
    void testNotify() {
        String message = "測試訊息";

        wsService.notify(message);

        verify(redisPublisher, times(1)).publish(message);
    }

    /**
     * 測試公共訊息發布到 Redis
     * 
     * @see com.hejz.springbootstomp.WsService#notify(String)
     */
    @Test
    @Order(2)
    void testNotifyPublishesToRedis() {
        String message = "測試訊息";

        wsService.notify(message);

        verify(redisPublisher, times(1)).publish(eq(message));
        verify(template, never()).convertAndSend(eq("/topic/message"), any(Object.class));
    }

    /**
     * 測試 null 訊息的公共訊息通知
     * 
     * @see com.hejz.springbootstomp.WsService#notify(String)
     */
    @Test
    @Order(3)
    void testNotifyWithNullMessage() {
        wsService.notify(null);

        verify(redisPublisher, times(1)).publish((String) null);
    }

    /**
     * 測試私信發送基本功能
     * 
     * @see com.hejz.springbootstomp.WsService#privateNotify(String, String)
     */
    @Test
    @Order(4)
    void testPrivateNotify() {
        String id = "user123";
        String message = "私信內容";

        wsService.privateNotify(id, message);

        verify(template, times(1)).convertAndSendToUser(
            eq(id),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
        verify(redisPublisher, never()).publish(anyString());
    }

    /**
     * 測試私信發送使用無效 ID
     * 
     * @see com.hejz.springbootstomp.WsService#privateNotify(String, String)
     */
    @Test
    @Order(5)
    void testPrivateNotifyWithInvalidId() {
        String id = null;
        String message = "私信內容";

        wsService.privateNotify(id, message);

        verify(template, times(1)).convertAndSendToUser(
            eq(id),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }

    /**
     * 測試 null 訊息的私信發送
     * 
     * @see com.hejz.springbootstomp.WsService#privateNotify(String, String)
     */
    @Test
    @Order(6)
    void testPrivateNotifyWithNullMessage() {
        String id = "user123";
        String message = null;

        wsService.privateNotify(id, message);

        verify(template, times(1)).convertAndSendToUser(
            eq(id),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }
}

