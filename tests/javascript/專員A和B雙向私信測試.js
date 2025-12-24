/**
 * 專員A和專員B雙向私信測試腳本
 * 測試專員A和專員B之間可以互相發送私信
 */

const SockJS = require('sockjs-client');
const Stomp = require('stompjs');
const http = require('http');

const BASE_URL = 'http://localhost:8080';
const WS_URL = 'http://localhost:8080/our-websocket';

// 測試結果
let testResults = {
    agentA: {
        connected: false,
        userId: null,
        subscribed: false,
        messagesSent: 0,
        messagesReceived: 0,
        receivedMessages: []
    },
    agentB: {
        connected: false,
        userId: null,
        subscribed: false,
        messagesSent: 0,
        messagesReceived: 0,
        receivedMessages: []
    },
    testPassed: false,
    errors: []
};

// 顏色輸出
const colors = {
    reset: '\x1b[0m',
    green: '\x1b[32m',
    red: '\x1b[31m',
    yellow: '\x1b[33m',
    cyan: '\x1b[36m',
    blue: '\x1b[34m',
    magenta: '\x1b[35m'
};

function log(message, color = 'reset') {
    console.log(`${colors[color]}${message}${colors.reset}`);
}

// 等待指定時間
function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

// 從API獲取專員ID
async function getAgentId(agentType) {
    return new Promise((resolve, reject) => {
        const req = http.get(`${BASE_URL}/api/agent/${agentType}`, (res) => {
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

// 建立 WebSocket 連接並返回 STOMP 客戶端（專員A）
function createAgentAStompClient() {
    return new Promise((resolve, reject) => {
        log(`\n[專員A] 正在建立 WebSocket 連接...`, 'cyan');
        
        // 使用查詢參數指定專員類型
        const socket = new SockJS(WS_URL + '?agentType=a');
        const client = Stomp.over(socket);
        
        // 禁用心跳（簡化測試）
        client.heartbeat.outgoing = 0;
        client.heartbeat.incoming = 0;
        
        // 啟用調試日誌
        client.debug = (str) => {
            if (str.includes('SEND') || str.includes('MESSAGE') || str.includes('ERROR') || str.includes('CONNECTED')) {
                log(`[專員A] STOMP: ${str}`, 'blue');
            }
        };
        
        client.connect({}, (frame) => {
            log(`[專員A] ✓ 連接成功！`, 'green');
            
            // 從連接幀的headers中提取用戶ID
            let userId = null;
            if (frame.headers && frame.headers['user-name']) {
                userId = frame.headers['user-name'];
                log(`[專員A] 用戶ID: ${userId}`, 'cyan');
            }
            
            // 監聽錯誤
            client.onerror = (error) => {
                log(`[專員A] ✗ STOMP 錯誤: ${error}`, 'red');
            };
            
            // 監聽斷開連接
            socket.onclose = () => {
                log(`[專員A] ⚠ WebSocket 連接已關閉`, 'yellow');
            };
            
            resolve({
                client: client,
                socket: socket,
                name: '專員A',
                frame: frame,
                userId: userId
            });
        }, (error) => {
            log(`[專員A] ✗ 連接失敗: ${error}`, 'red');
            reject(error);
        });
    });
}

// 建立 WebSocket 連接並返回 STOMP 客戶端（專員B）
function createAgentBStompClient() {
    return new Promise((resolve, reject) => {
        log(`\n[專員B] 正在建立 WebSocket 連接...`, 'cyan');
        
        // 使用查詢參數指定專員類型
        const socket = new SockJS(WS_URL + '?agentType=b');
        const client = Stomp.over(socket);
        
        // 禁用心跳（簡化測試）
        client.heartbeat.outgoing = 0;
        client.heartbeat.incoming = 0;
        
        // 啟用調試日誌
        client.debug = (str) => {
            if (str.includes('SEND') || str.includes('MESSAGE') || str.includes('ERROR') || str.includes('CONNECTED')) {
                log(`[專員B] STOMP: ${str}`, 'magenta');
            }
        };
        
        client.connect({}, (frame) => {
            log(`[專員B] ✓ 連接成功！`, 'green');
            
            // 從連接幀的headers中提取用戶ID
            let userId = null;
            if (frame.headers && frame.headers['user-name']) {
                userId = frame.headers['user-name'];
                log(`[專員B] 用戶ID: ${userId}`, 'cyan');
            }
            
            // 監聽錯誤
            client.onerror = (error) => {
                log(`[專員B] ✗ STOMP 錯誤: ${error}`, 'red');
            };
            
            // 監聽斷開連接
            socket.onclose = () => {
                log(`[專員B] ⚠ WebSocket 連接已關閉`, 'yellow');
            };
            
            resolve({
                client: client,
                socket: socket,
                name: '專員B',
                frame: frame,
                userId: userId
            });
        }, (error) => {
            log(`[專員B] ✗ 連接失敗: ${error}`, 'red');
            reject(error);
        });
    });
}

// 主測試函數
async function runTest() {
    log('\n========================================', 'cyan');
    log('  專員A和專員B雙向私信測試', 'green');
    log('========================================', 'cyan');
    log('測試目標：驗證專員A和專員B可以互相發送私信', 'yellow');
    log('');
    
    let agentA = null;
    let agentB = null;
    
    try {
        // 步驟1：建立專員A連接
        log('步驟1：建立專員A連接...', 'yellow');
        agentA = await createAgentAStompClient();
        testResults.agentA.connected = true;
        
        // 如果連接幀中沒有用戶ID，從API獲取
        if (!agentA.userId) {
            await sleep(1000);
            agentA.userId = await getAgentId('a');
            if (agentA.userId) {
                log(`[專員A] 從API獲取用戶ID: ${agentA.userId}`, 'cyan');
            }
        }
        
        if (!agentA.userId) {
            throw new Error('無法獲取專員A的ID');
        }
        testResults.agentA.userId = agentA.userId;
        
        // 步驟2：建立專員B連接
        log('\n步驟2：建立專員B連接...', 'yellow');
        agentB = await createAgentBStompClient();
        testResults.agentB.connected = true;
        
        // 如果連接幀中沒有用戶ID，從API獲取
        if (!agentB.userId) {
            await sleep(1000);
            agentB.userId = await getAgentId('b');
            if (agentB.userId) {
                log(`[專員B] 從API獲取用戶ID: ${agentB.userId}`, 'cyan');
            }
        }
        
        if (!agentB.userId) {
            throw new Error('無法獲取專員B的ID');
        }
        testResults.agentB.userId = agentB.userId;
        
        // 驗證兩個ID不同
        if (agentA.userId === agentB.userId) {
            throw new Error('專員A和專員B的ID應該不同');
        }
        
        log(`\n[專員A] ID: ${agentA.userId}`, 'green');
        log(`[專員B] ID: ${agentB.userId}`, 'green');
        
        // 步驟3：專員A訂閱私信頻道
        log('\n步驟3：專員A訂閱私信頻道...', 'yellow');
        agentA.client.subscribe('/user/topic/privateMessage', (message) => {
            log(`\n[專員A] ✓ 收到私信！`, 'green');
            log(`[專員A] 訊息內容: ${message.body}`, 'cyan');
            
            testResults.agentA.messagesReceived++;
            testResults.agentA.receivedMessages.push({
                body: message.body,
                timestamp: new Date().toISOString()
            });
            
            try {
                const data = JSON.parse(message.body);
                log(`[專員A] 發送者: ${data.sender || '未知'}`, 'cyan');
                log(`[專員A] 內容: ${data.content || message.body}`, 'cyan');
            } catch (e) {
                // 不是JSON格式，直接顯示
            }
        });
        testResults.agentA.subscribed = true;
        log('[專員A] ✓ 已訂閱 /user/topic/privateMessage', 'green');
        await sleep(1000);
        
        // 步驟4：專員B訂閱私信頻道
        log('\n步驟4：專員B訂閱私信頻道...', 'yellow');
        agentB.client.subscribe('/user/topic/privateMessage', (message) => {
            log(`\n[專員B] ✓ 收到私信！`, 'green');
            log(`[專員B] 訊息內容: ${message.body}`, 'cyan');
            
            testResults.agentB.messagesReceived++;
            testResults.agentB.receivedMessages.push({
                body: message.body,
                timestamp: new Date().toISOString()
            });
            
            try {
                const data = JSON.parse(message.body);
                log(`[專員B] 發送者: ${data.sender || '未知'}`, 'cyan');
                log(`[專員B] 內容: ${data.content || message.body}`, 'cyan');
            } catch (e) {
                // 不是JSON格式，直接顯示
            }
        });
        testResults.agentB.subscribed = true;
        log('[專員B] ✓ 已訂閱 /user/topic/privateMessage', 'green');
        await sleep(1000);
        
        // 步驟5：等待連接穩定
        log('\n步驟5：等待連接穩定...', 'yellow');
        await sleep(2000);
        
        // 步驟6：專員A發送私信給專員B
        log('\n步驟6：專員A發送私信給專員B...', 'yellow');
        const messageAtoB = {
            content: `專員A發送給專員B的測試訊息 - ${new Date().toLocaleTimeString('zh-TW')}`,
            id: agentB.userId
        };
        
        log(`[專員A] 發送訊息到 /ws/privateMessage`, 'cyan');
        log(`[專員A] 接收者ID: ${agentB.userId}`, 'cyan');
        log(`[專員A] 訊息內容: ${messageAtoB.content}`, 'cyan');
        
        agentA.client.send('/ws/privateMessage', {}, JSON.stringify(messageAtoB));
        testResults.agentA.messagesSent++;
        log('[專員A] ✓ 私信已發送', 'green');
        
        // 等待訊息送達
        await sleep(3000);
        
        // 步驟7：專員B發送私信給專員A
        log('\n步驟7：專員B發送私信給專員A...', 'yellow');
        const messageBtoA = {
            content: `專員B發送給專員A的測試訊息 - ${new Date().toLocaleTimeString('zh-TW')}`,
            id: agentA.userId
        };
        
        log(`[專員B] 發送訊息到 /ws/privateMessage`, 'cyan');
        log(`[專員B] 接收者ID: ${agentA.userId}`, 'cyan');
        log(`[專員B] 訊息內容: ${messageBtoA.content}`, 'cyan');
        
        agentB.client.send('/ws/privateMessage', {}, JSON.stringify(messageBtoA));
        testResults.agentB.messagesSent++;
        log('[專員B] ✓ 私信已發送', 'green');
        
        // 等待訊息送達
        await sleep(3000);
        
        // 步驟8：驗證測試結果
        log('\n步驟8：驗證測試結果...', 'yellow');
        
        const aReceivedB = testResults.agentA.messagesReceived > 0;
        const bReceivedA = testResults.agentB.messagesReceived > 0;
        
        log(`[專員A] 發送: ${testResults.agentA.messagesSent} 條`, 
            testResults.agentA.messagesSent > 0 ? 'green' : 'red');
        log(`[專員A] 接收: ${testResults.agentA.messagesReceived} 條`, 
            testResults.agentA.messagesReceived > 0 ? 'green' : 'red');
        log(`[專員B] 發送: ${testResults.agentB.messagesSent} 條`, 
            testResults.agentB.messagesSent > 0 ? 'green' : 'red');
        log(`[專員B] 接收: ${testResults.agentB.messagesReceived} 條`, 
            testResults.agentB.messagesReceived > 0 ? 'green' : 'red');
        
        // 測試通過條件：雙方都收到對方的訊息
        if (aReceivedB && bReceivedA) {
            testResults.testPassed = true;
            log('\n✓ 測試通過！專員A和專員B可以互相發送私信！', 'green');
        } else {
            if (!aReceivedB) {
                testResults.errors.push('專員A未收到專員B的私信');
                log('\n✗ 專員A未收到專員B的私信', 'red');
            }
            if (!bReceivedA) {
                testResults.errors.push('專員B未收到專員A的私信');
                log('\n✗ 專員B未收到專員A的私信', 'red');
            }
            log('\n✗ 測試失敗！', 'red');
        }
        
        // 清理：斷開連接
        log('\n清理：斷開連接...', 'yellow');
        if (agentA && agentA.client && agentA.client.connected) {
            agentA.client.disconnect();
        }
        if (agentB && agentB.client && agentB.client.connected) {
            agentB.client.disconnect();
        }
        
    } catch (error) {
        log(`\n✗ 測試過程中發生錯誤: ${error.message}`, 'red');
        testResults.errors.push(error.message);
        testResults.testPassed = false;
        
        // 清理
        if (agentA && agentA.client && agentA.client.connected) {
            agentA.client.disconnect();
        }
        if (agentB && agentB.client && agentB.client.connected) {
            agentB.client.disconnect();
        }
    }
    
    // 輸出測試結果
    log('\n========================================', 'cyan');
    log('  測試結果摘要', 'green');
    log('========================================', 'cyan');
    log(`專員A連接: ${testResults.agentA.connected ? '✓' : '✗'}`, 
        testResults.agentA.connected ? 'green' : 'red');
    log(`專員A ID: ${testResults.agentA.userId || 'N/A'}`, 'cyan');
    log(`專員A訂閱: ${testResults.agentA.subscribed ? '✓' : '✗'}`, 
        testResults.agentA.subscribed ? 'green' : 'red');
    log(`專員A發送: ${testResults.agentA.messagesSent} 條`, 'cyan');
    log(`專員A接收: ${testResults.agentA.messagesReceived} 條`, 'cyan');
    log('');
    log(`專員B連接: ${testResults.agentB.connected ? '✓' : '✗'}`, 
        testResults.agentB.connected ? 'green' : 'red');
    log(`專員B ID: ${testResults.agentB.userId || 'N/A'}`, 'cyan');
    log(`專員B訂閱: ${testResults.agentB.subscribed ? '✓' : '✗'}`, 
        testResults.agentB.subscribed ? 'green' : 'red');
    log(`專員B發送: ${testResults.agentB.messagesSent} 條`, 'cyan');
    log(`專員B接收: ${testResults.agentB.messagesReceived} 條`, 'cyan');
    log('');
    log(`測試通過: ${testResults.testPassed ? '✓' : '✗'}`, 
        testResults.testPassed ? 'green' : 'red');
    if (testResults.errors.length > 0) {
        log(`\n錯誤:`, 'red');
        testResults.errors.forEach(err => log(`  - ${err}`, 'red'));
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

