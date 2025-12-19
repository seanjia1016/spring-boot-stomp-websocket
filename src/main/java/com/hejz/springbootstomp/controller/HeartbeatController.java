package com.hejz.springbootstomp.controller;

import com.hejz.springbootstomp.config.RabbitMQConfig;
import com.hejz.springbootstomp.dto.ClientStatusCheckMessage;
import com.hejz.springbootstomp.dto.HeartbeatMessage;
import com.hejz.springbootstomp.dto.ResponseMessage;
import com.hejz.springbootstomp.service.ClientStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 心跳處理控制器
 * 
 * <p>此控制器負責處理客戶端發送的心跳請求，實現客戶端線上狀態管理。
 * 
 * <p>處理流程：
 * <ol>
 *   <li>接收客戶端心跳請求（透過 WebSocket 私信）</li>
 *   <li>記錄最後訪問時間到 Redis</li>
 *   <li>發送延遲訊息到 RabbitMQ（延遲時間 > 心跳間隔）</li>
 *   <li>返回心跳確認訊息給客戶端</li>
 * </ol>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Controller
public class HeartbeatController {

    @Autowired
    private ClientStatusService clientStatusService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 心跳間隔時間（毫秒），從配置檔案讀取
     */
    @Value("${client.status.heartbeat.interval:30000}")
    private long heartbeatInterval;

    /**
     * 檢查延遲時間（毫秒），從配置檔案讀取
     */
    @Value("${client.status.check.delay:60000}")
    private long checkDelay;

    /**
     * 處理客戶端心跳請求
     * 
     * <p>當客戶端透過 WebSocket 私信發送心跳請求時，此方法會：
     * <ol>
     *   <li>記錄客戶端最後訪問時間到 Redis</li>
     *   <li>發送延遲訊息到 RabbitMQ，用於檢查客戶端是否超時</li>
     *   <li>返回心跳確認訊息給客戶端</li>
     * </ol>
     * 
     * <p>訊息路由：
     * <ul>
     *   <li>接收：ws/heartbeat（客戶端發送心跳）</li>
     *   <li>回應：/user/topic/heartbeat（發送給特定客戶端）</li>
     * </ul>
     * 
     * @param principal 客戶端身份資訊（包含客戶端 ID）
     * @param heartbeatMessage 心跳訊息（可選，如果為 null 則使用 principal 的 ID）
     * @return 心跳確認訊息
     */
    @MessageMapping("/heartbeat")
    @SendToUser("/topic/heartbeat")
    public ResponseMessage handleHeartbeat(Principal principal, HeartbeatMessage heartbeatMessage) {
        // 獲取客戶端 ID（優先使用訊息中的 ID，否則使用 principal 的 ID）
        String clientId = (heartbeatMessage != null && heartbeatMessage.getClientId() != null) 
                ? heartbeatMessage.getClientId() 
                : principal.getName();
        
        log.debug("收到客戶端心跳：clientId={}", clientId);
        
        // 記錄客戶端心跳到 Redis
        Long lastHeartbeatTime = clientStatusService.recordHeartbeat(clientId);
        
        // 發送延遲訊息到 RabbitMQ，用於檢查客戶端是否超時
        // 延遲時間設定為檢查延遲時間（預設 60 秒）
        sendDelayedStatusCheck(clientId, lastHeartbeatTime);
        
        // 返回心跳確認訊息
        return new ResponseMessage("心跳已接收，最後心跳時間：" + lastHeartbeatTime);
    }

    /**
     * 發送延遲狀態檢查訊息到 RabbitMQ
     * 
     * <p>使用 TTL + 死信佇列的方式實現延遲訊息：
     * <ol>
     *   <li>將訊息發送到延遲佇列（帶 TTL）</li>
     *   <li>訊息在延遲佇列中等待 TTL 時間</li>
     *   <li>TTL 到期後，訊息自動轉發到死信交換器</li>
     *   <li>死信交換器將訊息路由到實際處理佇列</li>
     *   <li>消費者從處理佇列接收並處理訊息</li>
     * </ol>
     * 
     * <p>優點：
     * <ul>
     *   <li>不需要安裝任何外掛</li>
     *   <li>適用於所有 RabbitMQ 版本</li>
     *   <li>可以動態設定延遲時間</li>
     * </ul>
     * 
     * @param clientId 客戶端 ID
     * @param expectedLastHeartbeatTime 預期的最後心跳時間
     */
    private void sendDelayedStatusCheck(String clientId, Long expectedLastHeartbeatTime) {
        ClientStatusCheckMessage checkMessage = new ClientStatusCheckMessage(
                clientId,
                expectedLastHeartbeatTime,
                System.currentTimeMillis()
        );
        
        // 建立訊息後處理器，設定 TTL（延遲時間）
        MessagePostProcessor messagePostProcessor = new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) {
                // 設定訊息過期時間（TTL），單位：毫秒
                message.getMessageProperties().setExpiration(String.valueOf(checkDelay));
                return message;
            }
        };
        
        // 發送訊息到延遲佇列（直接發送到佇列，不經過交換器）
        // 訊息會在延遲佇列中等待 TTL 時間，然後轉發到死信交換器
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CLIENT_STATUS_CHECK_DELAY_QUEUE,
                checkMessage,
                messagePostProcessor
        );
        
        log.debug("發送延遲客戶端狀態檢查訊息：clientId={}, delay={}ms, expectedLastHeartbeatTime={}", 
                clientId, checkDelay, expectedLastHeartbeatTime);
    }
}

