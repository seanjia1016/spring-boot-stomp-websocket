package com.hejz.springbootstomp.consumer;

import com.hejz.springbootstomp.config.RabbitMQConfig;
import com.hejz.springbootstomp.dto.ClientStatusCheckMessage;
import com.hejz.springbootstomp.service.ClientStatusService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * 客戶端狀態檢查訊息消費者
 * 
 * <p>此消費者負責處理來自 RabbitMQ 的延遲訊息，檢查客戶端是否超時。
 * 
 * <p>工作流程：
 * <ol>
     *   <li>接收延遲訊息（延遲時間 > 心跳間隔）</li>
     *   <li>從 Redis 讀取客戶端最後心跳時間</li>
     *   <li>比較當前時間與最後心跳時間</li>
     *   <li>如果超時，更新客戶端狀態為「未連接」</li>
     * </ol>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Component
public class ClientStatusCheckConsumer {

    @Autowired
    private ClientStatusService clientStatusService;

    /**
     * 監聽客戶端狀態檢查佇列
     * 
     * <p>當延遲訊息到達時，此方法會被觸發，檢查客戶端是否超時。
     * 
     * <p>關於 @Header(AmqpHeaders.DELIVERY_TAG)：
     * <ul>
     *   <li>DELIVERY_TAG 是 RabbitMQ 為每條訊息分配的唯一標識符</li>
     *   <li>它是一個長整數，用於識別特定的訊息傳遞</li>
     *   <li>必須使用 deliveryTag 來手動確認（ACK）或拒絕（NACK）訊息</li>
     *   <li>在手動確認模式下（manual acknowledge），必須明確確認訊息，否則訊息會一直留在佇列中</li>
     * </ul>
     * 
     * <p>使用場景：
     * <ul>
     *   <li>channel.basicAck(deliveryTag, false) - 確認訊息已成功處理</li>
     *   <li>channel.basicNack(deliveryTag, false, true) - 拒絕訊息並重新入佇列</li>
     *   <li>channel.basicNack(deliveryTag, false, false) - 拒絕訊息並丟棄</li>
     * </ul>
     * 
     * @param message 客戶端狀態檢查訊息
     * @param channel RabbitMQ 頻道，用於手動確認或拒絕訊息
     * @param deliveryTag 訊息標籤（用於手動確認），這是 RabbitMQ 為每條訊息分配的唯一識別符
     *                    <ul>
     *                      <li>必須使用此標籤來確認訊息處理完成</li>
     *                      <li>如果不確認，訊息會一直留在佇列中，可能導致重複處理</li>
     *                      <li>在同一個頻道上，deliveryTag 是嚴格遞增的</li>
     *                    </ul>
     */
    @RabbitListener(queues = RabbitMQConfig.CLIENT_STATUS_CHECK_QUEUE)
    public void handleClientStatusCheck(
            ClientStatusCheckMessage message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        
        try {
            String clientId = message.getClientId();
            Long expectedLastHeartbeatTime = message.getExpectedLastHeartbeatTime();
            
            log.debug("處理客戶端狀態檢查：clientId={}, expectedLastHeartbeatTime={}", 
                    clientId, expectedLastHeartbeatTime);
            
            // 檢查客戶端是否超時
            boolean isTimeout = clientStatusService.checkClientTimeout(clientId, expectedLastHeartbeatTime);
            
            if (isTimeout) {
                log.info("客戶端已超時，狀態已更新為離線：clientId={}", clientId);
            } else {
                log.debug("客戶端仍在線：clientId={}", clientId);
            }
            
            // 手動確認訊息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            log.error("處理客戶端狀態檢查時發生錯誤：clientId={}, error={}", 
                    message.getClientId(), e.getMessage(), e);
            
            try {
                // 發生錯誤時，拒絕訊息並重新入佇列（可選）
                // channel.basicNack(deliveryTag, false, true);
                
                // 或者拒絕訊息並丟棄（避免無限重試）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("拒絕訊息時發生錯誤：{}", ex.getMessage(), ex);
            }
        }
    }
}

