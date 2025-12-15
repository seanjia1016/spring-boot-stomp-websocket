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
 * Notificationservice 單元測試類別
 * 
 * <p>此測試類別驗證伺服器端 WebSocket 通知服務的功能，包括：
 * <ul>
 *   <li>全局通知服務</li>
 *   <li>私信通知服務</li>
 *   <li>Redis 發布</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testGloubNotificationservice() - 驗證基本全局通知</li>
 *   <li>testGloubNotificationservicePublishesToRedis() - 驗證 Redis 發布</li>
 *   <li>testGloubNotificationserviceWithEmptyMessage() - 驗證空訊息處理</li>
 *   <li>testPrivateNotificationservice() - 驗證基本私信通知</li>
 *   <li>testPrivateNotificationserviceWithInvalidId() - 驗證無效 ID 處理</li>
 *   <li>testPrivateNotificationserviceWithEmptyMessage() - 驗證空訊息處理</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.Notificationservice
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class NotificationserviceTests {

    @Mock
    private SimpMessagingTemplate template;

    @Mock
    private RedisMessagePublisher redisPublisher;

    @InjectMocks
    private Notificationservice notificationservice;

    @BeforeEach
    void setUp() {
        // 設置預設行為
    }

    /**
     * 測試全局通知服務基本功能
     * 
     * @see com.hejz.springbootstomp.Notificationservice#gloubNotificationservice(String)
     */
    @Test
    @Order(1)
    void testGloubNotificationservice() {
        String message = "測試訊息";

        notificationservice.gloubNotificationservice(message);

        verify(redisPublisher, times(1)).publish(message);
    }

    /**
     * 測試全局通知服務發布到 Redis
     * 
     * @see com.hejz.springbootstomp.Notificationservice#gloubNotificationservice(String)
     */
    @Test
    @Order(2)
    void testGloubNotificationservicePublishesToRedis() {
        String message = "測試訊息";

        notificationservice.gloubNotificationservice(message);

        verify(redisPublisher, times(1)).publish(message);
        verify(template, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    /**
     * 測試空訊息的全局通知服務
     * 
     * @see com.hejz.springbootstomp.Notificationservice#gloubNotificationservice(String)
     */
    @Test
    @Order(3)
    void testGloubNotificationserviceWithEmptyMessage() {
        String message = "";

        notificationservice.gloubNotificationservice(message);

        verify(redisPublisher, times(1)).publish(message);
    }

    /**
     * 測試私信通知服務基本功能
     * 
     * @see com.hejz.springbootstomp.Notificationservice#privateNotificationservice(String, String)
     */
    @Test
    @Order(4)
    void testPrivateNotificationservice() {
        String id = "user123";
        String message = "私信內容";

        notificationservice.privateNotificationservice(id, message);

        verify(template, times(1)).convertAndSendToUser(
            eq(id),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
        verify(redisPublisher, never()).publish(anyString());
    }

    /**
     * 測試私信通知服務使用無效 ID
     * 
     * @see com.hejz.springbootstomp.Notificationservice#privateNotificationservice(String, String)
     */
    @Test
    @Order(5)
    void testPrivateNotificationserviceWithInvalidId() {
        String id = null;
        String message = "私信內容";

        notificationservice.privateNotificationservice(id, message);

        verify(template, times(1)).convertAndSendToUser(
            eq(id),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }

    /**
     * 測試空訊息的私信通知服務
     * 
     * @see com.hejz.springbootstomp.Notificationservice#privateNotificationservice(String, String)
     */
    @Test
    @Order(6)
    void testPrivateNotificationserviceWithEmptyMessage() {
        String id = "user123";
        String message = "";

        notificationservice.privateNotificationservice(id, message);

        verify(template, times(1)).convertAndSendToUser(
            eq(id),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }
}

