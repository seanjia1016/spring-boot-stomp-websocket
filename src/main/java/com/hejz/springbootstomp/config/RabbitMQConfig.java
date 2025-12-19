package com.hejz.springbootstomp.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置類
 * 
 * <p>此配置類負責配置 RabbitMQ 的佇列、交換器和路由規則，
 * 用於實現客戶端狀態管理的延遲訊息檢查功能。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>定義客戶端狀態檢查佇列</li>
 *   <li>配置訊息轉換器（JSON）</li>
 *   <li>配置 RabbitTemplate</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Configuration
public class RabbitMQConfig {

    /**
     * 客戶端狀態檢查佇列名稱（實際處理佇列）
     */
    public static final String CLIENT_STATUS_CHECK_QUEUE = "client.status.check.queue";

    /**
     * 客戶端狀態檢查延遲佇列名稱（帶 TTL 的佇列）
     */
    public static final String CLIENT_STATUS_CHECK_DELAY_QUEUE = "client.status.check.delay.queue";

    /**
     * 客戶端狀態檢查交換器名稱（實際處理交換器）
     */
    public static final String CLIENT_STATUS_CHECK_EXCHANGE = "client.status.check.exchange";

    /**
     * 客戶端狀態檢查死信交換器名稱（延遲佇列過期後轉發到這裡）
     */
    public static final String CLIENT_STATUS_CHECK_DLX_EXCHANGE = "client.status.check.dlx.exchange";

    /**
     * 客戶端狀態檢查路由鍵
     */
    public static final String CLIENT_STATUS_CHECK_ROUTING_KEY = "client.status.check";

    /**
     * 客戶端狀態檢查延遲路由鍵
     */
    public static final String CLIENT_STATUS_CHECK_DELAY_ROUTING_KEY = "client.status.check.delay";

    /**
     * 配置 JSON 訊息轉換器
     * 
     * @return Jackson2JsonMessageConverter 實例
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     * 
     * @param connectionFactory RabbitMQ 連接工廠
     * @return RabbitTemplate 實例
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    /**
     * 配置 RabbitListener 容器工廠
     * 
     * @param connectionFactory RabbitMQ 連接工廠
     * @return SimpleRabbitListenerContainerFactory 實例
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    /**
     * 定義客戶端狀態檢查死信交換器
     * 
     * <p>當延遲佇列中的訊息過期後，會轉發到此交換器，
     * 然後再轉發到實際的處理佇列。
     * 
     * @return DirectExchange 實例
     */
    @Bean
    public DirectExchange clientStatusCheckDlxExchange() {
        return new DirectExchange(CLIENT_STATUS_CHECK_DLX_EXCHANGE);
    }

    /**
     * 定義客戶端狀態檢查交換器（實際處理交換器）
     * 
     * <p>死信交換器會將過期的訊息轉發到此交換器，
     * 然後路由到實際的處理佇列。
     * 
     * @return DirectExchange 實例
     */
    @Bean
    public DirectExchange clientStatusCheckExchange() {
        return new DirectExchange(CLIENT_STATUS_CHECK_EXCHANGE);
    }

    /**
     * 定義客戶端狀態檢查佇列（實際處理佇列）
     * 
     * <p>這是真正處理客戶端狀態檢查的佇列，
     * 消費者會從此佇列接收訊息。
     * 
     * @return Queue 實例
     */
    @Bean
    public Queue clientStatusCheckQueue() {
        return QueueBuilder.durable(CLIENT_STATUS_CHECK_QUEUE).build();
    }

    /**
     * 定義客戶端狀態檢查延遲佇列（帶 TTL）
     * 
     * <p>此佇列用於暫存需要延遲處理的訊息。
     * 訊息在此佇列中等待 TTL 時間，過期後會轉發到死信交換器。
     * 
     * <p>配置說明：
     * <ul>
     *   <li>x-message-ttl: 訊息過期時間（毫秒），由發送時動態設定</li>
     *   <li>x-dead-letter-exchange: 死信交換器，過期後轉發到這裡</li>
     *   <li>x-dead-letter-routing-key: 死信路由鍵</li>
     * </ul>
     * 
     * @return Queue 實例
     */
    @Bean
    public Queue clientStatusCheckDelayQueue() {
        return QueueBuilder
                .durable(CLIENT_STATUS_CHECK_DELAY_QUEUE)
                // 設定死信交換器：當訊息過期後，轉發到此交換器
                .withArgument("x-dead-letter-exchange", CLIENT_STATUS_CHECK_DLX_EXCHANGE)
                // 設定死信路由鍵：過期訊息的路由鍵
                .withArgument("x-dead-letter-routing-key", CLIENT_STATUS_CHECK_ROUTING_KEY)
                .build();
    }

    /**
     * 綁定處理佇列到死信交換器
     * 
     * <p>當延遲佇列中的訊息過期後，會轉發到死信交換器，
     * 死信交換器再將訊息路由到處理佇列。
     * 
     * <p>注意：延遲佇列不需要綁定到交換器，因為訊息是直接發送到佇列的。
     * 延遲佇列配置了死信交換器和路由鍵，當訊息過期時會自動轉發。
     * 
     * @return Binding 實例
     */
    @Bean
    public Binding clientStatusCheckDlxBinding() {
        return BindingBuilder
                .bind(clientStatusCheckQueue())
                .to(clientStatusCheckDlxExchange())
                .with(CLIENT_STATUS_CHECK_ROUTING_KEY);
    }
}

