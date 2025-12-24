# 專員ID檢查機制說明

## 問題描述

當同一個專員（例如專員A）開啟多個網頁時：
1. 第一個網頁連接，獲得ID：`3b7936c43d06412fbe40a5bd002eccb6`
2. 第二個網頁連接，Redis中的ID被覆蓋為：`404c0dbe69a343409ddede0e2746532f`
3. **問題**：第一個網頁仍然顯示「已連線」，但實際上它的ID已經無效

## 解決方案

實作前端ID有效性檢查機制，定期檢查當前連接的ID是否與Redis中的ID匹配。

### 1. 新增API端點

**端點**：`GET /api/agent/{agentType}/check/{currentId}`

**功能**：檢查當前連接的ID是否為有效的專員ID

**回應格式**：
```json
{
  "success": true,
  "agentType": "a",
  "currentId": "3b7936c43d06412fbe40a5bd002eccb6",
  "validId": "404c0dbe69a343409ddede0e2746532f",
  "isValid": false,
  "message": "ID已變更，當前有效ID: 404c0dbe69a343409ddede0e2746532f"
}
```

### 2. 前端檢查機制

**實作位置**：`agent-a.html` 和 `agent-b.html`

**檢查邏輯**：
```javascript
// 檢查當前連接的ID是否有效（是否與Redis中的ID匹配）
async function checkAgentIdValidity() {
    if (!currentUserId) {
        return; // 如果還沒有連接，不檢查
    }
    
    try {
        const response = await fetch(`/api/agent/a/check/${currentUserId}`);
        const data = await response.json();
        
        if (data.success) {
            if (!data.isValid) {
                // ID已變更，當前連接無效
                // 1. 斷開連接
                // 2. 更新狀態為「已離線（ID已變更）」
                // 3. 顯示提示訊息
            }
        }
    } catch (error) {
        console.error('檢查專員ID有效性失敗:', error);
    }
}
```

**檢查頻率**：每3秒檢查一次

### 3. 工作流程

```
前端頁面載入
    ↓
建立 WebSocket 連接
    ↓
獲得專員ID（例如：3b7936c43d06412fbe40a5bd002eccb6）
    ↓
每3秒檢查一次ID有效性
    ↓
調用 GET /api/agent/a/check/3b7936c43d06412fbe40a5bd002eccb6
    ↓
後端從Redis獲取當前有效的ID（例如：404c0dbe69a343409ddede0e2746532f）
    ↓
比較：3b7936c43d06412fbe40a5bd002eccb6 ≠ 404c0dbe69a343409ddede0e2746532f
    ↓
返回 isValid: false
    ↓
前端檢測到ID無效
    ↓
1. 斷開 WebSocket 連接
2. 更新狀態為「已離線（ID已變更）」
3. 顯示提示訊息：「專員A的ID已變更，您的連接已斷開。請重新整理頁面。」
```

## 實作細節

### 後端實作

**文件**：`src/main/java/com/hejz/springbootstomp/controller/AgentController.java`

```java
@GetMapping("/{agentType}/check/{currentId}")
public ResponseEntity<Map<String, Object>> checkAgentId(
        @PathVariable String agentType,
        @PathVariable String currentId) {
    String validId = agentService.getAgentId(agentType);
    
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("agentType", agentType.toLowerCase());
    response.put("currentId", currentId);
    response.put("validId", validId);
    response.put("isValid", validId != null && validId.equals(currentId));
    
    // ... 設定訊息
    
    return ResponseEntity.ok(response);
}
```

### 前端實作

**文件**：`src/main/resources/static/agent-a.html` 和 `agent-b.html`

```javascript
// 頁面載入時啟動定期檢查
$(document).ready(function() {
    // ... 其他初始化代碼
    
    // 定期檢查當前連接的ID是否有效（每3秒）
    setInterval(checkAgentIdValidity, 3000);
});
```

## 效果

### 修改前

- 第一個專員A網頁：顯示「已連線」（即使ID已無效）
- 第二個專員A網頁：顯示「已連線」
- **問題**：兩個網頁都顯示連線，但只有一個是有效的

### 修改後

- 第一個專員A網頁：
  - 檢測到ID已變更
  - 自動斷開連接
  - 顯示「已離線（ID已變更）」
  - 提示用戶重新整理頁面
- 第二個專員A網頁：
  - ID有效，正常顯示「已連線」

## 注意事項

1. **檢查頻率**：每3秒檢查一次，平衡即時性和伺服器負載
2. **斷開連接**：當檢測到ID無效時，會自動斷開WebSocket連接
3. **用戶提示**：顯示明確的提示訊息，告知用戶ID已變更
4. **重新連接**：用戶需要重新整理頁面才能重新連接

## 與 RabbitMQ 的關係

雖然專案中有 RabbitMQ 的心跳檢測機制，但：
1. **前端沒有使用**：前端沒有發送心跳請求
2. **本機制更直接**：直接檢查Redis中的ID，更簡單有效
3. **可以結合使用**：未來可以結合RabbitMQ的心跳機制，實現更完整的狀態管理

## 測試方法

1. **開啟第一個專員A網頁**
   - 觀察ID和連接狀態
   - 確認顯示「已連線」

2. **開啟第二個專員A網頁**
   - 觀察第一個網頁的狀態
   - 應該在3秒內自動變為「已離線（ID已變更）」
   - 顯示提示訊息

3. **檢查Redis**
   ```powershell
   docker exec redis redis-cli GET "agent:a:id"
   ```
   - 應該顯示第二個網頁的ID

4. **檢查API**
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:8080/api/agent/a/check/第一個ID" | Select-Object -ExpandProperty Content
   ```
   - 應該返回 `"isValid": false`

---

**文檔版本**：1.0  
**最後更新**：2025-12-19









