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

    // 模擬 Redis 訊息發布器，用於發布訊息到 Redis
    @Mock
    private RedisMessagePublisher redisPublisher;

    // 模擬 WebSocket 訊息發送模板，用於發送訊息給 WebSocket 客戶端
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    // 模擬使用者身份物件，代表當前登入的使用者
    @Mock
    private Principal principal;

    // 要測試的訊息控制器實例（Mock 物件會自動注入）
    @InjectMocks
    private MessageController messageController;

    @BeforeEach
    void setUp() {
        // 只在需要時設置 principal.getName()
        // 這個方法在每個測試執行前都會被呼叫，用來初始化測試環境
    }

    /**
     * 測試公共訊息處理基本功能
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Message 物件（content: "測試訊息"）</li>
     *   <li>透過方法：message(message)</li>
     *   <li>預期結果：應該將訊息內容發布到 Redis，讓所有伺服器節點都能收到</li>
     * </ul>
     * 
     * <p>實際場景：當客戶端透過 WebSocket 發送公共訊息時，控制器會收到訊息，
     * 然後將訊息發布到 Redis，其他伺服器節點的監聽器會收到並轉發給各自的客戶端。
     * 
     * @see com.hejz.springbootstomp.MessageController#message(Message)
     */
    @Test
    @Order(1)
    void testMessage() throws InterruptedException {
        // 建立測試用的訊息物件
        Message message = new Message();
        // 設定訊息內容
        message.setContent("測試訊息");

        // 執行被測試的方法：處理公共訊息
        messageController.message(message);

        // 驗證：確認 redisPublisher.publish() 被呼叫了 1 次，且參數是 "測試訊息"
        // eq() 表示參數必須完全相等
        verify(redisPublisher, times(1)).publish(eq("測試訊息"));
    }

    /**
     * 測試公共訊息 HTML 轉義功能
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Message 物件（content: "&lt;script&gt;alert('XSS')&lt;/script&gt;"）</li>
     *   <li>透過方法：message(message)</li>
     *   <li>預期結果：應該將 HTML 標籤轉義成安全字元（&lt; 和 &gt;），防止 XSS 攻擊</li>
     * </ul>
     * 
     * <p>實際場景：當客戶端發送包含 HTML 標籤的訊息時，系統應該要轉義這些標籤，
     * 避免惡意腳本在瀏覽器中執行，這是防止 XSS（跨站腳本攻擊）的重要安全措施。
     * 
     * @see com.hejz.springbootstomp.MessageController#message(Message)
     */
    @Test
    @Order(2)
    void testMessageWithHtmlEscape() throws InterruptedException {
        // 建立測試用的訊息物件
        Message message = new Message();
        // 設定包含 HTML 標籤的訊息內容（這是潛在的 XSS 攻擊）
        message.setContent("<script>alert('XSS')</script>");

        // 執行被測試的方法：處理公共訊息（應該會轉義 HTML）
        messageController.message(message);

        // 驗證：確認 redisPublisher.publish() 被呼叫了 1 次
        // argThat() 用來驗證參數是否符合特定條件
        // 這裡驗證發布的內容包含轉義後的 HTML 標籤（&lt;script&gt; 和 &lt;/script&gt;）
        verify(redisPublisher, times(1)).publish(argThat((String content) -> 
            content != null && content.contains("&lt;script&gt;") && content.contains("&lt;/script&gt;")
        ));
    }

    /**
     * 測試公共訊息發布到 Redis
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Message 物件（content: "測試訊息"）</li>
     *   <li>透過方法：message(message)</li>
     *   <li>預期結果：應該將訊息發布到 Redis，讓其他伺服器節點也能收到</li>
     * </ul>
     * 
     * <p>實際場景：在多伺服器環境中，當一個伺服器收到訊息時，會發布到 Redis，
     * 其他伺服器節點的監聽器會收到這個訊息，然後轉發給各自的 WebSocket 客戶端。
     * 
     * @see com.hejz.springbootstomp.MessageController#message(Message)
     */
    @Test
    @Order(3)
    void testMessagePublishesToRedis() throws InterruptedException {
        // 建立測試用的訊息物件
        Message message = new Message();
        // 設定訊息內容
        message.setContent("測試訊息");

        // 執行被測試的方法：處理公共訊息
        messageController.message(message);

        // 驗證：確認 redisPublisher.publish() 被呼叫了 1 次，用來發布訊息到 Redis
        verify(redisPublisher, times(1)).publish(eq("測試訊息"));
    }

    /**
     * 測試私信處理基本功能
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Principal（當前使用者：user123）、Message 物件（content: "私信內容", id: "user456"）</li>
     *   <li>透過方法：privateMessage(principal, message)</li>
     *   <li>預期結果：應該將私信發送給 id 指定的使用者（user456），只有該使用者能收到</li>
     * </ul>
     * 
     * <p>實際場景：當客戶端發送私信時，控制器會收到訊息和使用者身份，
     * 然後將訊息發送給指定的接收者，只有該接收者能收到這則私信。
     * 
     * @see com.hejz.springbootstomp.MessageController#privateMessage(Principal, Message)
     */
    @Test
    @Order(4)
    void testPrivateMessage() throws InterruptedException {
        // 設定 Mock 行為：當呼叫 principal.getName() 時，返回 "user123"（發送者 ID）
        when(principal.getName()).thenReturn("user123");
        // 建立測試用的訊息物件
        Message message = new Message();
        // 設定私信內容
        message.setContent("私信內容");
        // 設定接收者 ID（使用 id 欄位）
        message.setId("user456");

        // 執行被測試的方法：處理私信
        messageController.privateMessage(principal, message);

        // 驗證：確認 messagingTemplate.convertAndSendToUser() 被呼叫了 1 次
        // 用來發送私信給指定的使用者（user456）
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("user456"),  // 接收者 ID
            eq("/topic/privateMessage"),  // 私信頻道
            any(ResponseMessage.class)  // 任何 ResponseMessage 物件
        );
    }

    /**
     * 測試使用 id 欄位的私信
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Principal（當前使用者：user123）、Message 物件（content: "私信內容", id: "user456", recipient: "user789"）</li>
     *   <li>透過方法：privateMessage(principal, message)</li>
     *   <li>預期結果：應該優先使用 id 欄位（user456）作為接收者，即使 recipient 欄位也有值</li>
     * </ul>
     * 
     * <p>實際場景：當訊息物件同時有 id 和 recipient 欄位時，系統應該優先使用 id 欄位，
     * 這是為了保持向後兼容性和明確的優先順序。
     * 
     * @see com.hejz.springbootstomp.MessageController#privateMessage(Principal, Message)
     */
    @Test
    @Order(5)
    void testPrivateMessageWithId() throws InterruptedException {
        // 設定 Mock 行為：當呼叫 principal.getName() 時，返回 "user123"
        when(principal.getName()).thenReturn("user123");
        // 建立測試用的訊息物件
        Message message = new Message();
        // 設定私信內容
        message.setContent("私信內容");
        // 設定接收者 ID（使用 id 欄位）
        message.setId("user456");
        // 設定接收者 ID（使用 recipient 欄位，但應該不會被使用）
        message.setRecipient("user789");

        // 執行被測試的方法：處理私信
        messageController.privateMessage(principal, message);

        // 應優先使用 id 欄位
        // 驗證：確認訊息發送給 id 指定的使用者（user456），而不是 recipient 指定的使用者
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("user456"),  // 應該使用 id 欄位
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }

    /**
     * 測試使用 recipient 欄位的私信
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：Principal（當前使用者：user123）、Message 物件（content: "私信內容", recipient: "user789"）</li>
     *   <li>透過方法：privateMessage(principal, message)</li>
     *   <li>預期結果：當 id 欄位為 null 時，應該使用 recipient 欄位（user789）作為接收者</li>
     * </ul>
     * 
     * <p>實際場景：當訊息物件只有 recipient 欄位有值，而 id 欄位為 null 時，
     * 系統應該使用 recipient 欄位來決定接收者，這是備用的接收者指定方式。
     * 
     * @see com.hejz.springbootstomp.MessageController#privateMessage(Principal, Message)
     */
    @Test
    @Order(6)
    void testPrivateMessageWithRecipient() throws InterruptedException {
        // 設定 Mock 行為：當呼叫 principal.getName() 時，返回 "user123"
        when(principal.getName()).thenReturn("user123");
        // 建立測試用的訊息物件
        Message message = new Message();
        // 設定私信內容
        message.setContent("私信內容");
        // 設定接收者 ID（使用 recipient 欄位，因為 id 為 null）
        message.setRecipient("user789");

        // 執行被測試的方法：處理私信
        messageController.privateMessage(principal, message);

        // 如果 id 為 null，應使用 recipient
        // 驗證：確認訊息發送給 recipient 指定的使用者（user789）
        verify(messagingTemplate, times(1)).convertAndSendToUser(
            eq("user789"),  // 應該使用 recipient 欄位
            eq("/topic/privateMessage"),
            any(ResponseMessage.class)
        );
    }
}

