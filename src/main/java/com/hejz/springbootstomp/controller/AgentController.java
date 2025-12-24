package com.hejz.springbootstomp.controller;

import com.hejz.springbootstomp.service.AgentService;
import com.hejz.springbootstomp.service.AgentStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 專員ID管理控制器
 * 
 * <p>提供API端點來獲取專員A和專員B的ID。
 * 
 * @author Spring Boot STOMP WebSocket Team
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentStatusService agentStatusService;

    /**
     * 獲取專員A的ID
     * 
     * @return 包含專員A ID和狀態的JSON回應
     */
    @GetMapping("/a")
    public ResponseEntity<Map<String, Object>> getAgentA() {
        try {
            String agentId = agentService.getAgentAId();
            String status = agentStatusService.getAgentAStatus();
            
            Map<String, Object> response = new HashMap<>();
            if (agentId != null) {
                response.put("success", true);
                response.put("agentType", "a");
                response.put("agentName", "專員A");
                response.put("agentId", agentId);
                response.put("status", status != null ? status : "OFFLINE");
            } else {
                response.put("success", false);
                response.put("message", "專員A尚未連接");
                response.put("status", "OFFLINE");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("獲取專員A ID時發生錯誤: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "獲取專員A ID時發生錯誤: " + e.getMessage());
            response.put("status", "OFFLINE");
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 獲取專員B的ID
     * 
     * @return 包含專員B ID和狀態的JSON回應
     */
    @GetMapping("/b")
    public ResponseEntity<Map<String, Object>> getAgentB() {
        String agentId = agentService.getAgentBId();
        String status = agentStatusService.getAgentBStatus();
        
        Map<String, Object> response = new HashMap<>();
        if (agentId != null) {
            response.put("success", true);
            response.put("agentType", "b");
            response.put("agentName", "專員B");
            response.put("agentId", agentId);
            response.put("status", status != null ? status : "OFFLINE");
        } else {
            response.put("success", false);
            response.put("message", "專員B尚未連接");
            response.put("status", "OFFLINE");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 根據專員類型獲取ID
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @return 包含專員ID的JSON回應
     */
    @GetMapping("/{agentType}")
    public ResponseEntity<Map<String, Object>> getAgent(@PathVariable String agentType) {
        String agentId = agentService.getAgentId(agentType);
        
        Map<String, Object> response = new HashMap<>();
        if (agentId != null) {
            response.put("success", true);
            response.put("agentType", agentType.toLowerCase());
            response.put("agentName", "a".equalsIgnoreCase(agentType) ? "專員A" : "專員B");
            response.put("agentId", agentId);
        } else {
            response.put("success", false);
            response.put("message", "專員" + (agentType.equalsIgnoreCase("a") ? "A" : "B") + "尚未連接");
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 檢查當前ID是否為有效的專員ID
     * 
     * @param agentType 專員類型（"a" 或 "b"）
     * @param currentId 當前連接的ID
     * @return 包含檢查結果的JSON回應
     */
    @GetMapping("/{agentType}/check/{currentId}")
    public ResponseEntity<Map<String, Object>> checkAgentId(
            @PathVariable String agentType,
            @PathVariable String currentId) {
        try {
            String validId = agentService.getAgentId(agentType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("agentType", agentType.toLowerCase());
            response.put("currentId", currentId);
            response.put("validId", validId);
            response.put("isValid", validId != null && validId.equals(currentId));
            
            if (validId == null) {
                response.put("message", "專員" + (agentType.equalsIgnoreCase("a") ? "A" : "B") + "尚未連接");
            } else if (!validId.equals(currentId)) {
                response.put("message", "ID已變更，當前有效ID: " + validId);
            } else {
                response.put("message", "ID有效");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("檢查專員ID時發生錯誤: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "檢查ID時發生錯誤: " + e.getMessage());
            response.put("isValid", false); // 發生錯誤時，視為無效
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 獲取專員A的狀態
     * 
     * @return 包含專員A狀態的JSON回應
     */
    @GetMapping("/a/status")
    public ResponseEntity<Map<String, Object>> getAgentAStatus() {
        String status = agentStatusService.getAgentAStatus();
        String agentId = agentService.getAgentAId();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("agentType", "a");
        response.put("agentName", "專員A");
        response.put("status", status != null ? status : "OFFLINE");
        response.put("agentId", agentId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 獲取專員B的狀態
     * 
     * @return 包含專員B狀態的JSON回應
     */
    @GetMapping("/b/status")
    public ResponseEntity<Map<String, Object>> getAgentBStatus() {
        String status = agentStatusService.getAgentBStatus();
        String agentId = agentService.getAgentBId();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("agentType", "b");
        response.put("agentName", "專員B");
        response.put("status", status != null ? status : "OFFLINE");
        response.put("agentId", agentId);
        
        return ResponseEntity.ok(response);
    }
}

