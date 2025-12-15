package com.hejz.springbootstomp;

import com.hejz.springbootstomp.dto.Message;
import com.hejz.springbootstomp.dto.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MessageController 單元測試類別
 * 
 * <p>此測試類別驗證 WebSocket 訊息控制器的功能，包括：
 * <ul>
 *   <li>公共訊息處理</li>
 *   <li>私信處理</li>
 *   <li>HTML 轉義</li>
 *   <li>Redis 發布</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testMessage() - 驗證基本公共訊息處理</li>
 *   <li>testMessageWithHtmlEscape() - 驗證 HTML 轉義</li>
 *   <li>testMessagePublishesToRedis() - 驗證 Redis 發布</li>
 *   <li>testPrivateMessage() - 驗證基本私信處理</li>
 *   <li>testPrivateMessageWithId() - 驗證使用 id 欄位的私信</li>
 *   <li>testPrivateMessageWithRecipient() - 驗證使用 recipient 欄位的私信</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.MessageController
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class MessageControllerTests {

    @Mock
    private RedisMessagePublisher redisPublisher;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private Principal principal;

    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    void setUp() {
        // 只在需要時設置 principal.getName()
    }

    /**
     * 測試公共訊息處理基本功能
     * 
     * @see com.hejz.springbootstomp.MessageController#message(Message)
     */
    @Test
    @Order(1)
    void testMessage() throws InterruptedException {
        Message message = new Message();
        message.setContent("測試訊息");

        messageController.message(message);

        verify(redisPublisher, times(1)).publish(eq("測試訊息"));
    }

    /**
     * 測試公共訊息 HTML 轉義功能
     * 
     * @see com.hejz.springbootstomp.MessageController#message(Message)
     */
    @Test
    @Order(2)
    void testMessageWithHtmlEscape() throws InterruptedException {
        Message message = new Message();
        message.setContent("<script>alert('XSS')</script>");

        messageController.message(message);

        verify(redisPublisher, times(1)).publish(argThat((String content) -> 
            content != null && content.contains("&lt;script&gt;") && content.contains("&lt;/script&gt;")
        ));
    }

    /**
     * 測試公共訊息發布到 Redis
     * 
     * @see com.hejz.springbootstomp.MessageController#message(Message)
     */
    @Test
    @Order(3)
    void testMessagePublishesToRedis() throws InterruptedException {
        Message message = new Message();
        message.setContent("測試訊息");

        messageController.message(message);

        verify(redisPublisher, times(1)).publish(eq("測試訊息"));
    }

    /**
     * 測試私信處理基本功能
     * 
     * @see com.hejz.springbootstomp.MessageController#privateMessage(Principal, Message)
     */
    @Test
    @Order(4)
    void testPrivateMessage() throws InterruptedException {
        when(principal.getName()).thenReturn("user123");
        Message message = new Message();
        message.setContent("私信內容");
        message.setId("user456");

        messageController.privateMessage(principal, message);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("user456"),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }

    /**
     * 測試使用 id 欄位的私信
     * 
     * @see com.hejz.springbootstomp.MessageController#privateMessage(Principal, Message)
     */
    @Test
    @Order(5)
    void testPrivateMessageWithId() throws InterruptedException {
        when(principal.getName()).thenReturn("user123");
        Message message = new Message();
        message.setContent("私信內容");
        message.setId("user456");
        message.setRecipient("user789");

        messageController.privateMessage(principal, message);

        // 應優先使用 id 欄位
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("user456"),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }

    /**
     * 測試使用 recipient 欄位的私信
     * 
     * @see com.hejz.springbootstomp.MessageController#privateMessage(Principal, Message)
     */
    @Test
    @Order(6)
    void testPrivateMessageWithRecipient() throws InterruptedException {
        when(principal.getName()).thenReturn("user123");
        Message message = new Message();
        message.setContent("私信內容");
        message.setRecipient("user789");

        messageController.privateMessage(principal, message);

        // 如果 id 為 null，應使用 recipient
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("user789"),
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }
}

