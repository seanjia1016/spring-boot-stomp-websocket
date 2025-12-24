package com.hejz.springbootstomp.controller;

import com.hejz.springbootstomp.dto.ChatMessage;
import com.hejz.springbootstomp.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天歷史記錄控制器
 * 
 * <p>此控制器提供 REST API 介面，允許客戶端查詢歷史聊天訊息。
 * 訊息從 Redis 中讀取，使用 Lua 腳本確保高效查詢。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>查詢公共訊息歷史記錄</li>
 *   <li>查詢私信歷史記錄</li>
 *   <li>支援分頁查詢</li>
 * </ul>
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatHistoryController {

    @Autowired
    private ChatMessageService chatMessageService;

    /**
     * 獲取公共訊息歷史記錄
     * 
     * <p>查詢參數：
     * <ul>
     *   <li>limit: 獲取數量（預設 50，最大 100）</li>
     *   <li>offset: 偏移量（預設 0，從最新開始）</li>
     * </ul>
     * 
     * <p>範例請求：
     * <pre>
     * GET /api/chat/public?limit=20&offset=0
     * </pre>
     * 
     * @param limit 獲取數量
     * @param offset 偏移量
     * @return 訊息列表（最新的在前）
     */
    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> getPublicMessages(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        try {
            // 限制最大查詢數量
            if (limit > 100) {
                limit = 100;
            }
            if (limit < 1) {
                limit = 50;
            }
            if (offset < 0) {
                offset = 0;
            }

            List<ChatMessage> messages = chatMessageService.getPublicMessages(limit, offset);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());
            response.put("limit", limit);
            response.put("offset", offset);
            
            log.info("查詢公共訊息歷史記錄：limit={}, offset={}, count={}", limit, offset, messages.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查詢公共訊息歷史記錄失敗: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 獲取私信歷史記錄
     * 
     * <p>查詢參數：
     * <ul>
     *   <li>userId: 用戶 ID（必填）</li>
     *   <li>limit: 獲取數量（預設 50，最大 100）</li>
     *   <li>offset: 偏移量（預設 0，從最新開始）</li>
     * </ul>
     * 
     * <p>範例請求：
     * <pre>
     * GET /api/chat/private?userId=user123&limit=20&offset=0
     * </pre>
     * 
     * @param userId 用戶 ID
     * @param limit 獲取數量
     * @param offset 偏移量
     * @return 訊息列表（最新的在前）
     */
    @GetMapping("/private")
    public ResponseEntity<Map<String, Object>> getPrivateMessages(
            @RequestParam String userId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        try {
            if (userId == null || userId.trim().isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", "userId 參數必填");
                return ResponseEntity.badRequest().body(response);
            }

            // 限制最大查詢數量
            if (limit > 100) {
                limit = 100;
            }
            if (limit < 1) {
                limit = 50;
            }
            if (offset < 0) {
                offset = 0;
            }

            List<ChatMessage> messages = chatMessageService.getPrivateMessages(userId, limit, offset);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messages", messages);
            response.put("count", messages.size());
            response.put("limit", limit);
            response.put("offset", offset);
            response.put("userId", userId);
            
            log.info("查詢私信歷史記錄：userId={}, limit={}, offset={}, count={}", userId, limit, offset, messages.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("查詢私信歷史記錄失敗: userId={}, error={}", userId, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.status(500).body(response);
        }
    }
}









