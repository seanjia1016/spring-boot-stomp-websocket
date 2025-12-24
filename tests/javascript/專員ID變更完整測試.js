/**
 * 專員ID變更完整測試腳本
 * 模擬兩個專員A連接，驗證第一個專員A是否會收到斷開通知
 */

const SockJS = require('sockjs-client');
const Stomp = require('stompjs');
const http = require('http');

const BASE_URL = 'http://localhost:8080';
const WS_URL = 'http://localhost:8080/our-websocket';

// 測試結果
let testResults = {
    firstAgent: {
        connected: false,
        userId: null,
        subscribed: false,
        receivedDisconnectNotification: false,
        disconnectMessage: null
    },
    secondAgent: {
        connected: false,
        userId: null,
        subscribed: false
    },
    testPassed: false,
    error: null
};

// 顏色輸出
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    cyan: '\x1b[36m',
    blue: '\x1b[34m'
};

function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

// 等待指定時間
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// 從API獲取專員A的ID
async function getAgentAId() {
    return new Promise((resolve, reject) => {
        const req = http.get(`${BASE_URL}/api/agent/a`, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    resolve(json.agentId || null);
                } catch (e) {
                    reject(e);
                }
            });
        });
        req.on('error', reject);
        req.setTimeout(5000, () => {
            req.destroy();
            reject(new Error('Request timeout'));
        });
    });
}

// 檢查專員ID是否有效
async function checkAgentIdValidity(agentType, agentId) {
    return new Promise((resolve, reject) => {
        const req = http.get(`${BASE_URL}/api/agent/${agentType}/check/${agentId}`, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const json = JSON.parse(data);
                    resolve(json.isValid || false);
                } catch (e) {
                    reject(e);
                }
            });
        });
        req.on('error', reject);
        req.setTimeout(5000, () => {
            req.destroy();
            reject(new Error('Request timeout'));
        });
    });
}

// 建立 WebSocket 連接並返回 STOMP 客戶端（專員A）
function createAgentAStompClient(name) {
    return new Promise((resolve, reject) => {
        log(`\n[${name}] 正在建立 WebSocket 連接（專員A）...`, 'cyan');
        
        // 使用查詢參數指定專員類型，讓伺服器能夠判斷是專員A
        const socket = new SockJS(WS_URL + '?agentType=a');
        const client = Stomp.over(socket);
        
        // 禁用心跳（簡化測試）
        client.heartbeat.outgoing = 0;
        client.heartbeat.incoming = 0;
        
        // 啟用調試日誌
        client.debug = (str) => {
            if (str.includes('SEND') || str.includes('MESSAGE') || str.includes('ERROR') || str.includes('CONNECTED')) {
                log(`[${name}] STOMP: ${str}`, 'blue');
            }
        };
        
        client.connect({}, (frame) => {
            log(`[${name}] ✓ 連接成功！`, 'green');
            
            // 從連接幀的headers中提取用戶ID
            let userId = null;
            if (frame.headers && frame.headers['user-name']) {
                userId = frame.headers['user-name'];
                log(`[${name}] 用戶ID: ${userId}`, 'cyan');
            } else {
                // 如果headers中沒有，嘗試從其他方式獲取
                log(`[${name}] ⚠ 無法從連接幀中獲取用戶ID，將從API獲取`, 'yellow');
            }
            
            // 監聽錯誤
            client.onerror = (error) => {
                log(`[${name}] ✗ STOMP 錯誤: ${error}`, 'red');
            };
            
            // 監聽斷開連接
            socket.onclose = () => {
                log(`[${name}] ⚠ WebSocket 連接已關閉`, 'yellow');
            };
            
            resolve({
                client: client,
                socket: socket,
                name: name,
                frame: frame,
                userId: userId
            });
        }, (error) => {
            log(`[${name}] ✗ 連接失敗: ${error}`, 'red');
            reject(error);
        });
    });
}

// 主測試函數
async function runTest() {
    log('\n========================================', 'cyan');
    log('專員ID變更完整測試', 'green');
    log('========================================', 'cyan');
    log('');
    
    let firstClient = null;
    let secondClient = null;
    
    try {
        // 步驟1：清除Redis中的專員A ID（通過API）
        log('步驟1：清除Redis中的專員A ID...', 'cyan');
        // 注意：實際清除需要通過Redis API或直接操作Redis
        // 這裡我們假設測試環境是乾淨的，或者第一個連接會覆蓋現有ID
        
        // 步驟2：第一個專員A連接
        log('\n步驟2：第一個專員A連接...', 'cyan');
        firstClient = await createAgentAStompClient('第一個專員A');
        testResults.firstAgent.connected = true;
        
        // 如果連接幀中沒有用戶ID，從API獲取
        if (!firstClient.userId) {
            await sleep(1000); // 等待ID創建
            firstClient.userId = await getAgentAId();
            if (firstClient.userId) {
                log(`[第一個專員A] 從API獲取用戶ID: ${firstClient.userId}`, 'cyan');
            }
        }
        
        if (!firstClient.userId) {
            throw new Error('無法獲取第一個專員A的ID');
        }
        
        testResults.firstAgent.userId = firstClient.userId;
        
        // 訂閱ID變更通知頻道
        log('\n步驟3：第一個專員A訂閱ID變更通知頻道...', 'cyan');
        const subscription = firstClient.client.subscribe('/user/topic/agentIdChanged', (message) => {
            log(`[第一個專員A] ✓ 收到ID變更通知！`, 'green');
            log(`[第一個專員A] 通知內容: ${message.body}`, 'cyan');
            log(`[第一個專員A] 訊息ID: ${message.headers['message-id']}`, 'cyan');
            log(`[第一個專員A] 訂閱ID: ${message.headers['subscription']}`, 'cyan');
            
            testResults.firstAgent.receivedDisconnectNotification = true;
            try {
                const data = JSON.parse(message.body);
                testResults.firstAgent.disconnectMessage = data;
            } catch (e) {
                testResults.firstAgent.disconnectMessage = message.body;
            }
        }, {
            'id': 'agentIdChangeSubscription'
        });
        testResults.firstAgent.subscribed = true;
        log('[第一個專員A] ✓ 已訂閱 /user/topic/agentIdChanged', 'green');
        log(`[第一個專員A] 訂閱ID: ${subscription.id}`, 'cyan');
        
        // 等待一下，確保訂閱完成和ID存儲到Redis
        log('\n步驟4：等待ID存儲到Redis...', 'cyan');
        await sleep(2000);
        
        // 驗證第一個專員A的ID有效
        log('\n步驟5：驗證第一個專員A的ID...', 'cyan');
        let firstIdValid = false;
        let retryCount = 0;
        while (!firstIdValid && retryCount < 5) {
            firstIdValid = await checkAgentIdValidity('a', firstClient.userId);
            if (!firstIdValid) {
                log(`[第一個專員A] ID檢查失敗，重試 ${retryCount + 1}/5...`, 'yellow');
                await sleep(1000);
                retryCount++;
            }
        }
        
        if (!firstIdValid) {
            log('[第一個專員A] ⚠ ID檢查失敗，但繼續測試（可能是API延遲）', 'yellow');
        } else {
            log('[第一個專員A] ✓ ID有效', 'green');
        }
        
        // 步驟6：第二個專員A連接（觸發ID覆蓋）
        log('\n步驟6：第二個專員A連接（將觸發ID覆蓋）...', 'cyan');
        secondClient = await createAgentAStompClient('第二個專員A');
        testResults.secondAgent.connected = true;
        
        // 如果連接幀中沒有用戶ID，從API獲取
        if (!secondClient.userId) {
            await sleep(2000); // 等待ID創建和RabbitMQ通知
            secondClient.userId = await getAgentAId();
            if (secondClient.userId) {
                log(`[第二個專員A] 從API獲取用戶ID: ${secondClient.userId}`, 'cyan');
            }
        }
        
        if (!secondClient.userId) {
            throw new Error('無法獲取第二個專員A的ID');
        }
        
        testResults.secondAgent.userId = secondClient.userId;
        
        // 驗證兩個ID不同
        if (firstClient.userId === secondClient.userId) {
            throw new Error('兩個專員A的ID應該不同');
        }
        log(`[第二個專員A] ✓ ID: ${secondClient.userId}`, 'green');
        log(`[第二個專員A] ✓ ID已覆蓋第一個專員A的ID`, 'green');
        
        // 等待RabbitMQ通知發送和處理
        log('\n步驟7：等待RabbitMQ通知發送和處理...', 'cyan');
        log('[系統] 等待5秒，確保RabbitMQ通知已發送和處理...', 'yellow');
        await sleep(5000);
        
        // 步驟8：驗證第一個專員A的ID已變為無效
        log('\n步驟8：驗證第一個專員A的ID狀態...', 'cyan');
        const firstIdStillValid = await checkAgentIdValidity('a', firstClient.userId);
        if (firstIdStillValid) {
            log('[第一個專員A] ⚠ ID仍然有效（可能RabbitMQ通知尚未處理）', 'yellow');
        } else {
            log('[第一個專員A] ✓ ID已變為無效（預期結果）', 'green');
        }
        
        // 步驟9：驗證第一個專員A是否收到斷開通知
        log('\n步驟9：驗證第一個專員A是否收到斷開通知...', 'cyan');
        if (testResults.firstAgent.receivedDisconnectNotification) {
            log('[第一個專員A] ✓ 已收到ID變更通知（預期結果）', 'green');
            testResults.testPassed = true;
        } else {
            log('[第一個專員A] ✗ 未收到ID變更通知（測試失敗）', 'red');
            testResults.testPassed = false;
            testResults.error = '第一個專員A未收到ID變更通知';
        }
        
        // 清理：斷開連接
        log('\n清理：斷開連接...', 'cyan');
        if (firstClient && firstClient.client && firstClient.client.connected) {
            firstClient.client.disconnect();
        }
        if (secondClient && secondClient.client && secondClient.client.connected) {
            secondClient.client.disconnect();
        }
        
    } catch (error) {
        log(`\n✗ 測試過程中發生錯誤: ${error.message}`, 'red');
        testResults.error = error.message;
        testResults.testPassed = false;
        
        // 清理
        if (firstClient && firstClient.client && firstClient.client.connected) {
            firstClient.client.disconnect();
        }
        if (secondClient && secondClient.client && secondClient.client.connected) {
            secondClient.client.disconnect();
        }
    }
    
    // 輸出測試結果
    log('\n========================================', 'cyan');
    log('測試結果', 'green');
    log('========================================', 'cyan');
    log(`第一個專員A連接: ${testResults.firstAgent.connected ? '✓' : '✗'}`, 
        testResults.firstAgent.connected ? 'green' : 'red');
    log(`第一個專員A ID: ${testResults.firstAgent.userId || 'N/A'}`, 'cyan');
    log(`第一個專員A訂閱: ${testResults.firstAgent.subscribed ? '✓' : '✗'}`, 
        testResults.firstAgent.subscribed ? 'green' : 'red');
    log(`第一個專員A收到通知: ${testResults.firstAgent.receivedDisconnectNotification ? '✓' : '✗'}`, 
        testResults.firstAgent.receivedDisconnectNotification ? 'green' : 'red');
    log(`第二個專員A連接: ${testResults.secondAgent.connected ? '✓' : '✗'}`, 
        testResults.secondAgent.connected ? 'green' : 'red');
    log(`第二個專員A ID: ${testResults.secondAgent.userId || 'N/A'}`, 'cyan');
    log(`測試通過: ${testResults.testPassed ? '✓' : '✗'}`, 
        testResults.testPassed ? 'green' : 'red');
    if (testResults.error) {
        log(`錯誤: ${testResults.error}`, 'red');
    }
    log('========================================\n', 'cyan');
    
    // 返回測試結果（用於自動化腳本）
    process.exit(testResults.testPassed ? 0 : 1);
}

// 執行測試
runTest().catch(error => {
    log(`\n✗ 測試執行失敗: ${error.message}`, 'red');
    console.error(error);
    process.exit(1);
});

