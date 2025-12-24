package com.hejz.springbootstomp.consumer;

import com.hejz.springbootstomp.dto.AgentIdChangeMessage;
import com.hejz.springbootstomp.dto.ResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AgentIdChangeConsumer 單元測試類別
 * 
 * <p>此測試類別驗證 RabbitMQ 專員ID變更訊息消費者的功能，包括：
 * <ul>
 *   <li>接收專員ID變更通知</li>
 *   <li>通過 WebSocket 發送斷開連接通知給舊的連接</li>
 *   <li>異常處理</li>
 * </ul>
 * 
 * <p>測試執行順序：
 * <ol>
 *   <li>testHandleAgentIdChange() - 驗證正常處理ID變更通知</li>
 *   <li>testHandleAgentIdChangeWithNullMessage() - 驗證 null 訊息處理</li>
 *   <li>testHandleAgentIdChangeWithException() - 驗證異常處理</li>
 *   <li>testHandleAgentIdChangeForAgentA() - 驗證專員A的ID變更</li>
 *   <li>testHandleAgentIdChangeForAgentB() - 驗證專員B的ID變更</li>
 * </ol>
 * 
 * @see com.hejz.springbootstomp.consumer.AgentIdChangeConsumer
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
class AgentIdChangeConsumerTests {

    // 模擬 WebSocket 訊息模板，用於發送通知給舊的連接
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    // 要測試的專員ID變更消費者實例
    @InjectMocks
    private AgentIdChangeConsumer agentIdChangeConsumer;

    /**
     * 測試正常處理專員ID變更通知
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：AgentIdChangeMessage（專員A，舊ID: "old-id-123"，新ID: "new-id-456"）</li>
     *   <li>透過方法：handleAgentIdChange(message)</li>
     *   <li>預期結果：應該通過 WebSocket 發送通知給舊的連接（old-id-123）</li>
     * </ul>
     * 
     * <p>實際場景：當專員A的ID被覆蓋時，系統會發送訊息到 RabbitMQ，
     * 此消費者接收訊息後，會通過 WebSocket 通知舊的連接斷開。
     * 
     * @see com.hejz.springbootstomp.consumer.AgentIdChangeConsumer#handleAgentIdChange(AgentIdChangeMessage)
     */
    @Test
    @Order(1)
    void testHandleAgentIdChange() {
        // 建立測試用的專員ID變更訊息
        AgentIdChangeMessage message = new AgentIdChangeMessage(
                "a",                    // agentType
                "old-id-123",           // oldId
                "new-id-456",           // newId
                "專員A",                 // agentName
                System.currentTimeMillis() // timestamp
        );

        // 執行被測試的方法：處理專員ID變更通知
        agentIdChangeConsumer.handleAgentIdChange(message);

        // 驗證：確認 messagingTemplate.convertAndSendToUser() 被呼叫了 1 次
        // 參數1：oldId（舊的專員ID）
        // 參數2：destination（目標路由：/topic/agentIdChanged）
        // 參數3：ResponseMessage（通知內容）
        ArgumentCaptor<ResponseMessage> messageCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("old-id-123"),
                eq("/topic/agentIdChanged"),
                messageCaptor.capture()
        );

        // 驗證：確認通知內容包含預期的資訊
        ResponseMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertTrue(capturedMessage.getContent().contains("專員A"));
        assertTrue(capturedMessage.getContent().contains("old-id-123"));
        assertTrue(capturedMessage.getContent().contains("new-id-456"));
    }

    /**
     * 測試處理 null 訊息
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：null 訊息</li>
     *   <li>透過方法：handleAgentIdChange(null)</li>
     *   <li>預期結果：應該能處理 null 值，不會拋出異常</li>
     * </ul>
     * 
     * <p>實際場景：當接收到 null 訊息時，系統應該要能正常處理，不會崩潰。
     * 
     * @see com.hejz.springbootstomp.consumer.AgentIdChangeConsumer#handleAgentIdChange(AgentIdChangeMessage)
     */
    @Test
    @Order(2)
    void testHandleAgentIdChangeWithNullMessage() {
        // 不應拋出異常（異常會被內部捕獲並記錄）
        try {
            // 執行被測試的方法：處理 null 訊息
            agentIdChangeConsumer.handleAgentIdChange(null);
        } catch (Exception e) {
            // 如果拋出異常，測試失敗（因為方法內部應該要捕獲異常）
            fail("不應拋出異常: " + e.getMessage());
        }

        // 驗證：確認 messagingTemplate.convertAndSendToUser() 沒有被呼叫
        // （因為訊息為 null，不應該發送通知）
        verify(messagingTemplate, never()).convertAndSendToUser(
                anyString(), anyString(), any(ResponseMessage.class));
    }

    /**
     * 測試異常處理
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：AgentIdChangeMessage（正常訊息）</li>
     *   <li>透過方法：handleAgentIdChange(message)</li>
     *   <li>預期結果：當發送通知失敗時，應該捕獲異常，不會拋出異常</li>
     * </ul>
     * 
     * <p>實際場景：當 WebSocket 發送通知失敗時（例如連接已斷開），
     * 系統應該要能處理這個錯誤，避免整個應用程式崩潰。
     * 
     * @see com.hejz.springbootstomp.consumer.AgentIdChangeConsumer#handleAgentIdChange(AgentIdChangeMessage)
     */
    @Test
    @Order(3)
    void testHandleAgentIdChangeWithException() {
        // 建立測試用的專員ID變更訊息
        AgentIdChangeMessage message = new AgentIdChangeMessage(
                "a", "old-id-123", "new-id-456", "專員A", System.currentTimeMillis());

        // 設定 Mock 行為：當呼叫 convertAndSendToUser() 時，拋出異常
        doThrow(new RuntimeException("WebSocket發送失敗")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any(ResponseMessage.class));

        // 不應拋出異常（異常會被內部捕獲並記錄）
        try {
            // 執行被測試的方法：處理專員ID變更通知（應該會失敗，但不會拋出異常）
            agentIdChangeConsumer.handleAgentIdChange(message);
        } catch (Exception e) {
            // 如果拋出異常，測試失敗（因為方法內部應該要捕獲異常）
            fail("不應拋出異常: " + e.getMessage());
        }

        // 驗證：確認 messagingTemplate.convertAndSendToUser() 被呼叫了 1 次（嘗試發送）
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                anyString(), anyString(), any(ResponseMessage.class));
    }

    /**
     * 測試專員A的ID變更
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：AgentIdChangeMessage（專員A）</li>
     *   <li>透過方法：handleAgentIdChange(message)</li>
     *   <li>預期結果：應該發送通知給舊的專員A連接</li>
     * </ul>
     * 
     * <p>實際場景：當專員A的ID被覆蓋時，系統會通知舊的專員A連接斷開。
     * 
     * @see com.hejz.springbootstomp.consumer.AgentIdChangeConsumer#handleAgentIdChange(AgentIdChangeMessage)
     */
    @Test
    @Order(4)
    void testHandleAgentIdChangeForAgentA() {
        // 建立測試用的專員A ID變更訊息
        AgentIdChangeMessage message = new AgentIdChangeMessage(
                "a",                    // agentType
                "agent-a-old-id",       // oldId
                "agent-a-new-id",       // newId
                "專員A",                 // agentName
                System.currentTimeMillis() // timestamp
        );

        // 執行被測試的方法：處理專員A ID變更通知
        agentIdChangeConsumer.handleAgentIdChange(message);

        // 驗證：確認發送通知給舊的專員A連接
        ArgumentCaptor<ResponseMessage> messageCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("agent-a-old-id"),
                eq("/topic/agentIdChanged"),
                messageCaptor.capture()
        );

        // 驗證：確認通知內容正確
        ResponseMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertTrue(capturedMessage.getContent().contains("專員A"));
        assertTrue(capturedMessage.getContent().contains("agent-a-old-id"));
        assertTrue(capturedMessage.getContent().contains("agent-a-new-id"));
    }

    /**
     * 測試專員B的ID變更
     * 
     * <p>範例說明：
     * <ul>
     *   <li>預期參數：AgentIdChangeMessage（專員B）</li>
     *   <li>透過方法：handleAgentIdChange(message)</li>
     *   <li>預期結果：應該發送通知給舊的專員B連接</li>
     * </ul>
     * 
     * <p>實際場景：當專員B的ID被覆蓋時，系統會通知舊的專員B連接斷開。
     * 
     * @see com.hejz.springbootstomp.consumer.AgentIdChangeConsumer#handleAgentIdChange(AgentIdChangeMessage)
     */
    @Test
    @Order(5)
    void testHandleAgentIdChangeForAgentB() {
        // 建立測試用的專員B ID變更訊息
        AgentIdChangeMessage message = new AgentIdChangeMessage(
                "b",                    // agentType
                "agent-b-old-id",       // oldId
                "agent-b-new-id",       // newId
                "專員B",                 // agentName
                System.currentTimeMillis() // timestamp
        );

        // 執行被測試的方法：處理專員B ID變更通知
        agentIdChangeConsumer.handleAgentIdChange(message);

        // 驗證：確認發送通知給舊的專員B連接
        ArgumentCaptor<ResponseMessage> messageCaptor = ArgumentCaptor.forClass(ResponseMessage.class);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("agent-b-old-id"),
                eq("/topic/agentIdChanged"),
                messageCaptor.capture()
        );

        // 驗證：確認通知內容正確
        ResponseMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);
        assertTrue(capturedMessage.getContent().contains("專員B"));
        assertTrue(capturedMessage.getContent().contains("agent-b-old-id"));
        assertTrue(capturedMessage.getContent().contains("agent-b-new-id"));
    }
}








