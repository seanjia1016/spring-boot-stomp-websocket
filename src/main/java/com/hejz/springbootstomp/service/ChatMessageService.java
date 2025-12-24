package com.hejz.springbootstomp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hejz.springbootstomp.dto.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天訊息持久化服務
 * 
 * <p>此服務負責將聊天訊息持久化到 Redis，使用 Lua 腳本確保原子性操作。
 * 訊息存儲在 Redis Sorted Set 中，以時間戳作為分數，確保訊息按時間排序。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>保存公共訊息到 Redis</li>
 *   <li>保存私信訊息到 Redis</li>
 *   <li>查詢歷史訊息（支援分頁）</li>
 *   <li>自動清理舊訊息（保留最近 N 條）</li>
 * </ul>
 * 
 * <p>存儲結構：
 * <ul>
 *   <li>公共訊息：chat:messages:public（Sorted Set）</li>
 *   <li>私信訊息：chat:messages:private:{userId}（Sorted Set）</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@Service
public class ChatMessageService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 公共訊息列表的 Redis Key
     */
    private static final String PUBLIC_MESSAGES_KEY = "chat:messages:public";

    /**
     * 私信訊息列表的 Redis Key 前綴
     */
    private static final String PRIVATE_MESSAGES_KEY_PREFIX = "chat:messages:private:";

    /**
     * 最大保留訊息數量（每種類型）
     */
    private static final int MAX_MESSAGES = 1000;

    /**
     * 預設查詢訊息數量
     */
    private static final int DEFAULT_LIMIT = 50;

    /**
     * 載入 Lua 腳本
     */
    private DefaultRedisScript<Long> saveMessageScript;
    private DefaultRedisScript<List> getMessagesScript;

    /**
     * 初始化 Lua 腳本
     */
    public ChatMessageService() {
        try {
            // 載入保存訊息的 Lua 腳本
            ClassPathResource saveScriptResource = new ClassPathResource("lua/save_message.lua");
            String saveScript = StreamUtils.copyToString(saveScriptResource.getInputStream(), StandardCharsets.UTF_8);
            saveMessageScript = new DefaultRedisScript<>(saveScript, Long.class);
            saveMessageScript.setResultType(Long.class);

            // 載入獲取訊息的 Lua 腳本
            ClassPathResource getScriptResource = new ClassPathResource("lua/get_messages.lua");
            String getScript = StreamUtils.copyToString(getScriptResource.getInputStream(), StandardCharsets.UTF_8);
            getMessagesScript = new DefaultRedisScript<>(getScript, List.class);
            getMessagesScript.setResultType(List.class);
        } catch (Exception e) {
            log.error("載入 Lua 腳本失敗: {}", e.getMessage(), e);
        }
    }

    /**
     * 保存公共訊息
     * 
     * <p>使用 Lua 腳本原子性地保存訊息到 Redis，確保：
     * <ul>
     *   <li>訊息按時間戳排序</li>
     *   <li>自動清理超過最大數量的舊訊息</li>
     *   <li>設置過期時間（30天）</li>
     * </ul>
     * 
     * @param senderId 發送者 ID
     * @param senderName 發送者名稱（可選）
     * @param content 訊息內容
     * @return 保存後的訊息總數
     */
    public Long savePublicMessage(String senderId, String senderName, String content) {
        try {
            long timestamp = System.currentTimeMillis();
            
            ChatMessage message = new ChatMessage(
                    senderId,
                    senderName != null ? senderName : senderId,
                    content,
                    timestamp,
                    "public",
                    null
            );

            String messageJson = objectMapper.writeValueAsString(message);
            
            // 使用 Lua 腳本原子性地保存訊息
            Long count = redisTemplate.execute(
                    saveMessageScript,
                    Collections.singletonList(PUBLIC_MESSAGES_KEY),
                    messageJson,
                    String.valueOf(timestamp),
                    String.valueOf(MAX_MESSAGES)
            );

            log.info("公共訊息已保存到 Redis：senderId={}, count={}", senderId, count);
            return count;
        } catch (Exception e) {
            log.error("保存公共訊息失敗: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 保存私信訊息
     * 
     * <p>為發送者和接收者分別保存私信訊息，確保雙方都能查看歷史記錄。
     * 
     * @param senderId 發送者 ID
     * @param senderName 發送者名稱（可選）
     * @param recipientId 接收者 ID
     * @param content 訊息內容
     * @return 保存後的訊息總數（發送者的）
     */
    public Long savePrivateMessage(String senderId, String senderName, String recipientId, String content) {
        try {
            long timestamp = System.currentTimeMillis();
            
            // 為發送者保存訊息
            ChatMessage senderMessage = new ChatMessage(
                    senderId,
                    senderName != null ? senderName : senderId,
                    content,
                    timestamp,
                    "private",
                    recipientId
            );

            // 為接收者保存訊息
            ChatMessage recipientMessage = new ChatMessage(
                    senderId,
                    senderName != null ? senderName : senderId,
                    content,
                    timestamp,
                    "private",
                    recipientId
            );

            String senderMessageJson = objectMapper.writeValueAsString(senderMessage);
            String recipientMessageJson = objectMapper.writeValueAsString(recipientMessage);
            
            // 使用 Lua 腳本原子性地保存訊息（發送者）
            Long senderCount = redisTemplate.execute(
                    saveMessageScript,
                    Collections.singletonList(PRIVATE_MESSAGES_KEY_PREFIX + senderId),
                    senderMessageJson,
                    String.valueOf(timestamp),
                    String.valueOf(MAX_MESSAGES)
            );

            // 使用 Lua 腳本原子性地保存訊息（接收者）
            Long recipientCount = redisTemplate.execute(
                    saveMessageScript,
                    Collections.singletonList(PRIVATE_MESSAGES_KEY_PREFIX + recipientId),
                    recipientMessageJson,
                    String.valueOf(timestamp),
                    String.valueOf(MAX_MESSAGES)
            );

            log.info("私信訊息已保存到 Redis：senderId={}, recipientId={}, senderCount={}, recipientCount={}", 
                    senderId, recipientId, senderCount, recipientCount);
            return senderCount;
        } catch (Exception e) {
            log.error("保存私信訊息失敗: {}", e.getMessage(), e);
            return 0L;
        }
    }

    /**
     * 獲取公共訊息歷史記錄
     * 
     * @param limit 獲取數量（預設 50）
     * @param offset 偏移量（預設 0，從最新開始）
     * @return 訊息列表（最新的在前）
     */
    public List<ChatMessage> getPublicMessages(int limit, int offset) {
        try {
            // 使用 Lua 腳本獲取訊息（反轉順序，最新的在前）
            List<Object> messageJsons = redisTemplate.execute(
                    getMessagesScript,
                    Collections.singletonList(PUBLIC_MESSAGES_KEY),
                    String.valueOf(offset),
                    String.valueOf(offset + limit - 1),
                    "1" // 反轉順序
            );

            if (messageJsons == null || messageJsons.isEmpty()) {
                return Collections.emptyList();
            }

            // 將 JSON 字串轉換為 ChatMessage 物件
            return messageJsons.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json.toString(), ChatMessage.class);
                        } catch (Exception e) {
                            log.error("解析訊息 JSON 失敗: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("獲取公共訊息失敗: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 獲取私信歷史記錄
     * 
     * @param userId 用戶 ID
     * @param limit 獲取數量（預設 50）
     * @param offset 偏移量（預設 0，從最新開始）
     * @return 訊息列表（最新的在前）
     */
    public List<ChatMessage> getPrivateMessages(String userId, int limit, int offset) {
        try {
            String key = PRIVATE_MESSAGES_KEY_PREFIX + userId;
            
            // 使用 Lua 腳本獲取訊息（反轉順序，最新的在前）
            List<Object> messageJsons = redisTemplate.execute(
                    getMessagesScript,
                    Collections.singletonList(key),
                    String.valueOf(offset),
                    String.valueOf(offset + limit - 1),
                    "1" // 反轉順序
            );

            if (messageJsons == null || messageJsons.isEmpty()) {
                return Collections.emptyList();
            }

            // 將 JSON 字串轉換為 ChatMessage 物件
            return messageJsons.stream()
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json.toString(), ChatMessage.class);
                        } catch (Exception e) {
                            log.error("解析訊息 JSON 失敗: {}", e.getMessage());
                            return null;
                        }
                    })
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("獲取私信訊息失敗: userId={}, error={}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 獲取公共訊息歷史記錄（使用預設參數）
     */
    public List<ChatMessage> getPublicMessages() {
        return getPublicMessages(DEFAULT_LIMIT, 0);
    }

    /**
     * 獲取私信歷史記錄（使用預設參數）
     */
    public List<ChatMessage> getPrivateMessages(String userId) {
        return getPrivateMessages(userId, DEFAULT_LIMIT, 0);
    }
}









