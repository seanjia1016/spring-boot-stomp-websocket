package com.hejz.springbootstomp.consumer;

import com.hejz.springbootstomp.config.RabbitMQConfig;
import com.hejz.springbootstomp.dto.AgentIdChangeMessage;
import com.hejz.springbootstomp.dto.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 專員ID變更訊息消費者
 * 
 * <p>此消費者負責處理來自 RabbitMQ 的專員ID變更通知。
 * 當專員ID被覆蓋時，會通過 WebSocket 發送通知給舊的連接，
 * 要求其斷開連接。
 * 
 * <p>工作流程：
 * <ol>
 *   <li>接收專員ID變更通知（來自 RabbitMQ）</li>
 *   <li>通過 WebSocket 發送斷開連接通知給舊的ID</li>
 *   <li>舊的連接收到通知後，前端會自動斷開連接</li>
 * </ol>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Component
public class AgentIdChangeConsumer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 監聽專員ID變更通知佇列
     * 
     * <p>當專員ID被覆蓋時，此方法會被觸發，通過 WebSocket 發送通知給舊的連接。
     * 
     * <p>訊息路由：
     * <ul>
     *   <li>接收：agent.id.change.queue（來自 RabbitMQ）</li>
     *   <li>發送：/user/{oldId}/topic/agentIdChanged（發送給舊的專員連接）</li>
     * </ul>
     * 
     * @param message 專員ID變更訊息
     */
    @RabbitListener(queues = RabbitMQConfig.AGENT_ID_CHANGE_QUEUE)
    public void handleAgentIdChange(AgentIdChangeMessage message) {
        try {
            String agentType = message.getAgentType();
            String oldId = message.getOldId();
            String newId = message.getNewId();
            String agentName = message.getAgentName();
            
            log.info("處理專員ID變更通知: {} ({} -> {})", agentName, oldId, newId);
            
            // 通過 WebSocket 發送通知給舊的連接
            // 使用 /user/{userId}/topic/agentIdChanged 路由
            String notificationContent = String.format(
                    "【系統通知】您的專員ID已變更，請重新整理頁面。舊ID: %s，新ID: %s", 
                    oldId, newId);
            ResponseMessage notification = new ResponseMessage(notificationContent);
            
            String destination = "/user/" + oldId + "/topic/agentIdChanged";
            messagingTemplate.convertAndSendToUser(oldId, "/topic/agentIdChanged", notification);
            
            log.info("已發送ID變更通知給舊連接: {} (destination: {})", oldId, destination);
            
        } catch (Exception e) {
            log.error("處理專員ID變更通知時發生錯誤: {}", e.getMessage(), e);
        }
    }
}

