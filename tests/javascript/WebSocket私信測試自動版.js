/**
 * WebSocket 私信測試腳本（自動版 - 從文件讀取用戶 ID）
 * 自動從臨時文件讀取用戶 ID 並測試私信功能
 */

const SockJS = require('sockjs-client');
const Stomp = require('stompjs');
const fs = require('fs');
const path = require('path');
const os = require('os');

const BASE_URL = 'http://localhost:8080';
const WS_URL = 'http://localhost:8080/our-websocket';

// 測試結果
let testResults = {
    agentA: {
        connected: false,
        userId: null,
        subscribed: false
    },
    agentB: {
        connected: false,
        userId: null,
        subscribed: false,
        messageReceived: false,
        receivedMessage: null
    },
    testPassed: false
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

// 從臨時文件讀取最新的用戶 ID
function readUserIdsFromFile() {
    try {
        const tempDir = os.tmpdir();
        const tempFile = path.join(tempDir, 'websocket_user_ids.json');
        
        if (!fs.existsSync(tempFile)) {
            return [];
        }
        
        const content = fs.readFileSync(tempFile, 'utf8');
        const lines = content.trim().split('\n').filter(line => line.trim());
        
        return lines.map(line => {
            try {
                return JSON.parse(line);
            } catch (e) {
                return null;
            }
        }).filter(item => item !== null);
    } catch (error) {
        log(`讀取用戶 ID 文件失敗: ${error.message}`, 'yellow');
        return [];
    }
}

// 獲取最新的兩個用戶 ID
function getLatestUserIds() {
    const userIds = readUserIdsFromFile();
    
    // 按時間戳排序，獲取最新的兩個
    userIds.sort((a, b) => b.timestamp - a.timestamp);
    
    return userIds.slice(0, 2).map(item => item.userId);
}

// 建立 WebSocket 連接並返回 STOMP 客戶端
function createStompClient(name) {
    return new Promise((resolve, reject) => {
        log(`\n[${name}] 正在建立 WebSocket 連接...`, 'cyan');
        
        const socket = new SockJS(WS_URL);
        const client = Stomp.over(socket);
        
        // 啟用調試日誌
        client.debug = (str) => {
            if (str.includes('SEND') || str.includes('MESSAGE') || str.includes('ERROR')) {
                log(`[${name}] STOMP: ${str}`, 'cyan');
            }
        };
        
        // 禁用心跳（簡化測試）
        client.heartbeat.outgoing = 0;
        client.heartbeat.incoming = 0;
        
        client.connect({}, (frame) => {
            log(`[${name}] ✓ 連接成功！`, 'green');
            log(`[${name}] 連接幀: ${JSON.stringify(frame)}`, 'cyan');
            
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
                frame: frame
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
    log('  WebSocket 私信測試（自動版）', 'cyan');
    log('========================================', 'cyan');
    log('測試目標：A 專員發送私信給 B 專員，檢查 B 是否收到', 'yellow');
    log('\n此腳本會自動從臨時文件讀取用戶 ID', 'cyan');
    
    // 清空之前的用戶 ID 記錄（可選）
    const tempDir = os.tmpdir();
    const tempFile = path.join(tempDir, 'websocket_user_ids.json');
    if (fs.existsSync(tempFile)) {
        try {
            fs.unlinkSync(tempFile);
            log('已清空之前的用戶 ID 記錄', 'yellow');
        } catch (e) {
            // 忽略錯誤
        }
    }
    
    let agentA = null;
    let agentB = null;
    
    try {
        // 1. 建立 A 專員連接
        log('\n[步驟 1] 建立 A 專員連接...', 'yellow');
        agentA = await createStompClient('A專員');
        testResults.agentA.connected = true;
        await sleep(2000); // 等待伺服器記錄用戶 ID
        
        // 2. 建立 B 專員連接
        log('\n[步驟 2] 建立 B 專員連接...', 'yellow');
        agentB = await createStompClient('B專員');
        testResults.agentB.connected = true;
        await sleep(2000); // 等待伺服器記錄用戶 ID
        
        // 3. 從文件讀取用戶 ID
        log('\n[步驟 3] 從臨時文件讀取用戶 ID...', 'yellow');
        const latestUserIds = getLatestUserIds();
        
        if (latestUserIds.length < 2) {
            log('\n✗ 無法獲取足夠的用戶 ID', 'red');
            log(`只找到 ${latestUserIds.length} 個用戶 ID，需要 2 個`, 'yellow');
            log('\n可能的原因：', 'yellow');
            log('1. 伺服器未正確寫入用戶 ID 到臨時文件', 'yellow');
            log('2. 文件路徑不正確', 'yellow');
            log('\n臨時文件路徑:', 'cyan');
            log(`  ${tempFile}`, 'cyan');
            log('\n請檢查應用程式日誌，確認是否有「登入用戶 ID」的記錄', 'yellow');
            
            // 清理
            if (agentA && agentA.client) {
                agentA.client.disconnect();
            }
            if (agentB && agentB.client) {
                agentB.client.disconnect();
            }
            
            return testResults;
        }
        
        // 注意：數組是按時間戳降序排列的，所以 latestUserIds[0] 是最新的（B 專員）
        // latestUserIds[1] 是較早的（A 專員）
        const bUserId = latestUserIds[0]; // 最新的（B 專員，第二個連接）
        const aUserId = latestUserIds[1]; // 較早的（A 專員，第一個連接）
        
        testResults.agentA.userId = aUserId;
        testResults.agentB.userId = bUserId;
        
        log(`[A專員] 用戶 ID: ${aUserId}`, 'cyan');
        log(`[B專員] 用戶 ID: ${bUserId}`, 'cyan');
        
        // 4. B 專員訂閱私信頻道
        log('\n[步驟 4] B 專員訂閱私信頻道...', 'yellow');
        agentB.client.subscribe('/user/topic/privateMessage', (message) => {
            log(`\n[B專員] ✓ 收到私信！`, 'green');
            log(`[B專員] 訊息內容: ${message.body}`, 'cyan');
            
            testResults.agentB.messageReceived = true;
            testResults.agentB.receivedMessage = message.body;
            
            try {
                const data = JSON.parse(message.body);
                log(`[B專員] 解析後的訊息:`, 'cyan');
                console.log(JSON.stringify(data, null, 2));
            } catch (e) {
                log(`[B專員] 訊息不是 JSON 格式`, 'yellow');
            }
        });
        testResults.agentB.subscribed = true;
        log('[B專員] ✓ 已訂閱 /user/topic/privateMessage', 'green');
        await sleep(1000);
        
        // 5. 檢查連接狀態
        log(`\n[步驟 5] 檢查連接狀態...`, 'yellow');
        if (!agentA.client.connected) {
            log('[A專員] ✗ 連接已斷開！', 'red');
            throw new Error('A專員連接已斷開');
        }
        if (!agentB.client.connected) {
            log('[B專員] ✗ 連接已斷開！', 'red');
            throw new Error('B專員連接已斷開');
        }
        log('[A專員] ✓ 連接正常', 'green');
        log('[B專員] ✓ 連接正常', 'green');
        await sleep(500);
        
        // 6. 等待連接穩定
        log(`\n[步驟 6] 等待連接穩定...`, 'yellow');
        await sleep(2000); // 等待 2 秒確保連接穩定
        
        // 再次確認連接狀態
        if (!agentA.client.connected || !agentB.client.connected) {
            log('✗ 連接不穩定，A專員連接: ' + agentA.client.connected + ', B專員連接: ' + agentB.client.connected, 'red');
            throw new Error('連接不穩定');
        }
        log('✓ 連接穩定', 'green');
        
        // 7. A 專員發送私信給 B 專員
        log(`\n[步驟 7] A 專員發送私信給 B 專員...`, 'yellow');
        const privateMessage = {
            content: `測試私信 - ${new Date().toLocaleTimeString('zh-TW')}`,
            id: bUserId
        };
        
        log(`[A專員] 發送訊息到 /ws/privateMessage`, 'cyan');
        log(`[A專員] 訊息內容:`, 'cyan');
        console.log(JSON.stringify(privateMessage, null, 2));
        
        try {
            // 再次檢查連接狀態
            if (!agentA.client.connected) {
                log('[A專員] ✗ 連接已斷開，無法發送', 'red');
                throw new Error('A專員連接已斷開');
            }
            
            log('[A專員] 準備發送，連接狀態: ' + agentA.client.connected, 'cyan');
            log('[A專員] WebSocket 狀態: ' + (agentA.socket.readyState === 1 ? 'OPEN' : 'CLOSED'), 'cyan');
            
            // 確保 WebSocket 連接是打開的
            if (agentA.socket.readyState !== 1) {
                log('[A專員] ✗ WebSocket 連接未打開，無法發送', 'red');
                throw new Error('WebSocket 連接未打開');
            }
            
            // 發送私信 - 使用正確的 STOMP 格式
            log('[A專員] 調用 STOMP send...', 'cyan');
            log('[A專員] 發送路徑: /ws/privateMessage', 'cyan');
            log('[A專員] 發送內容: ' + JSON.stringify(privateMessage), 'cyan');
            
            // 使用 STOMP 客戶端的 send 方法
            const sendResult = agentA.client.send('/ws/privateMessage', {}, JSON.stringify(privateMessage));
            log('[A專員] ✓ STOMP send 已調用，返回值: ' + (sendResult !== undefined ? sendResult : 'undefined'), 'green');
            
            // 檢查是否有錯誤
            if (agentA.client.ws && agentA.client.ws.readyState !== 1) {
                log('[A專員] ⚠ WebSocket 狀態異常: ' + agentA.client.ws.readyState, 'yellow');
            }
            
            // 等待一小段時間，確保訊息被處理
            await sleep(2000); // 增加到 2 秒
            
            // 再次檢查連接狀態
            if (!agentA.client.connected) {
                log('[A專員] ⚠ 發送後連接斷開', 'yellow');
            } else {
                log('[A專員] ✓ 發送後連接仍然正常', 'green');
            }
            
            log('[A專員] 請檢查伺服器日誌，確認是否有「私信接收開始」的記錄', 'cyan');
        } catch (error) {
            log(`[A專員] ✗ 發送失敗: ${error.message}`, 'red');
            log(`[A專員] 錯誤詳情: ${error.stack}`, 'red');
            throw error;
        }
        
        // 7. 等待 B 專員接收訊息
        log('\n[步驟 7] 等待 B 專員接收訊息（最多 10 秒）...', 'yellow');
        let waitTime = 0;
        const maxWait = 10000; // 增加到 10 秒
        const checkInterval = 500;
        
        while (waitTime < maxWait && !testResults.agentB.messageReceived) {
            await sleep(checkInterval);
            waitTime += checkInterval;
            process.stdout.write('.');
        }
        console.log('');
        
        // 7. 檢查測試結果
        log('\n========================================', 'cyan');
        log('  測試結果', 'cyan');
        log('========================================', 'cyan');
        
        log(`\nA 專員連接: ${testResults.agentA.connected ? '✓' : '✗'}`, 
            testResults.agentA.connected ? 'green' : 'red');
        log(`B 專員連接: ${testResults.agentB.connected ? '✓' : '✗'}`, 
            testResults.agentB.connected ? 'green' : 'red');
        log(`B 專員訂閱: ${testResults.agentB.subscribed ? '✓' : '✗'}`, 
            testResults.agentB.subscribed ? 'green' : 'red');
        log(`B 專員收到訊息: ${testResults.agentB.messageReceived ? '✓' : '✗'}`, 
            testResults.agentB.messageReceived ? 'green' : 'red');
        
        if (testResults.agentB.messageReceived) {
            log(`\n收到的訊息: ${testResults.agentB.receivedMessage}`, 'cyan');
            testResults.testPassed = true;
            log('\n✓ 測試通過！B 專員成功收到私信！', 'green');
        } else {
            log('\n✗ 測試失敗！B 專員未收到私信', 'red');
            log('\n可能的原因：', 'yellow');
            log('1. 用戶 ID 不正確', 'yellow');
            log('2. B 專員未正確訂閱私信頻道', 'yellow');
            log('3. 伺服器端私信處理有問題', 'yellow');
            log('4. 網路延遲（可以增加等待時間）', 'yellow');
            log('\n建議：', 'cyan');
            log('- 檢查應用程式控制台的日誌，確認私信是否被處理', 'cyan');
            log('- 查看是否有「=== 私信發送調試 ===」的日誌', 'cyan');
            log('- 確認用戶 ID 是否正確', 'cyan');
        }
        
        // 清理
        if (agentA && agentA.client) {
            agentA.client.disconnect();
        }
        if (agentB && agentB.client) {
            agentB.client.disconnect();
        }
        
        return testResults;
        
    } catch (error) {
        log(`\n測試過程中發生錯誤: ${error.message}`, 'red');
        log(error.stack, 'red');
        
        // 清理
        if (agentA && agentA.client) {
            agentA.client.disconnect();
        }
        if (agentB && agentB.client) {
            agentB.client.disconnect();
        }
        
        throw error;
    }
}

// 執行測試
if (require.main === module) {
    runTest()
        .then((results) => {
            process.exit(results.testPassed ? 0 : 1);
        })
        .catch((error) => {
            console.error('測試失敗:', error);
            process.exit(1);
        });
}

module.exports = { runTest };
