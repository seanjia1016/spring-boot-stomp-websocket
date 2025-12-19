/**
 * WebSocket 私信測試腳本（完整版）
 * 自動建立連接，從應用程式控制台獲取用戶 ID 並測試
 */

const SockJS = require('sockjs-client');
const Stomp = require('stompjs');
const readline = require('readline');

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

// 建立 WebSocket 連接並返回 STOMP 客戶端
function createStompClient(name) {
    return new Promise((resolve, reject) => {
        log(`\n[${name}] 正在建立 WebSocket 連接...`, 'cyan');
        
        const socket = new SockJS(WS_URL);
        const client = Stomp.over(socket);
        
        // 禁用心跳（簡化測試）
        client.heartbeat.outgoing = 0;
        client.heartbeat.incoming = 0;
        
        client.connect({}, (frame) => {
            log(`[${name}] ✓ 連接成功！`, 'green');
            
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

// 讀取用戶輸入
function getUserInput(question) {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });
    
    return new Promise((resolve) => {
        rl.question(question, (answer) => {
            rl.close();
            resolve(answer.trim());
        });
    });
}

// 主測試函數
async function runTest() {
    log('\n========================================', 'cyan');
    log('  WebSocket 私信測試（完整版）', 'cyan');
    log('========================================', 'cyan');
    log('測試目標：A 專員發送私信給 B 專員，檢查 B 是否收到', 'yellow');
    
    let agentA = null;
    let agentB = null;
    
    try {
        // 1. 建立 A 專員連接
        log('\n[步驟 1] 建立 A 專員連接...', 'yellow');
        agentA = await createStompClient('A專員');
        testResults.agentA.connected = true;
        
        log('\n請查看應用程式控制台（運行 Spring Boot 的窗口）', 'blue');
        log('找到類似這樣的日誌：', 'blue');
        log('  登入用戶 ID: abc123def456...', 'blue');
        log('這應該是 A 專員的用戶 ID', 'blue');
        
        await sleep(3000); // 等待伺服器記錄日誌
        
        // 2. 建立 B 專員連接
        log('\n[步驟 2] 建立 B 專員連接...', 'yellow');
        agentB = await createStompClient('B專員');
        testResults.agentB.connected = true;
        
        log('\n請再次查看應用程式控制台', 'blue');
        log('應該會看到第二個「登入用戶 ID」的日誌', 'blue');
        log('這應該是 B 專員的用戶 ID（32位字串，沒有連字號）', 'blue');
        
        await sleep(3000); // 等待伺服器記錄日誌
        
        // 3. B 專員訂閱私信頻道
        log('\n[步驟 3] B 專員訂閱私信頻道...', 'yellow');
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
        
        // 4. 獲取 B 專員的用戶 ID
        log('\n[步驟 4] 獲取 B 專員的用戶 ID', 'yellow');
        log('\n========================================', 'blue');
        log('  重要：請從應用程式控制台複製用戶 ID', 'blue');
        log('========================================', 'blue');
        log('\n1. 找到應用程式控制台（運行 Spring Boot 的窗口）', 'yellow');
        log('2. 找到最新的「登入用戶 ID: xxxxxx」日誌（應該是 B 專員的）', 'yellow');
        log('3. 複製該 ID（32位字串，例如：a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6）', 'yellow');
        log('4. 貼上到下方', 'yellow');
        log('\n提示：如果看到兩個「登入用戶 ID」，第二個是 B 專員的', 'cyan');
        
        const bUserId = await getUserInput('\n請輸入 B 專員的用戶 ID（或按 Enter 跳過）: ');
        
        if (!bUserId || bUserId.length === 0) {
            log('\n跳過自動測試', 'yellow');
            log('\n請手動在瀏覽器中測試：', 'cyan');
            log('1. 開啟 http://localhost:8080/agent-a.html', 'cyan');
            log('2. 開啟 http://localhost:8080/agent-b.html', 'cyan');
            log('3. 在專員A頁面查看「我的用戶ID」', 'cyan');
            log('4. 在專員B頁面查看「我的用戶ID」', 'cyan');
            log('5. 在專員A頁面輸入專員B的用戶ID，然後發送私信', 'cyan');
            
            // 清理
            if (agentA && agentA.client) {
                agentA.client.disconnect();
            }
            if (agentB && agentB.client) {
                agentB.client.disconnect();
            }
            
            return testResults;
        }
        
        // 驗證用戶 ID 格式（應該是 32 位字串）
        if (bUserId.length !== 32) {
            log(`\n警告：用戶 ID 長度為 ${bUserId.length}，預期為 32 位`, 'yellow');
            log('將繼續測試，但可能無法正常工作', 'yellow');
        }
        
        testResults.agentB.userId = bUserId;
        log(`\n使用 B 專員的用戶 ID: ${bUserId}`, 'green');
        
        // 5. A 專員發送私信給 B 專員
        log(`\n[步驟 5] A 專員發送私信給 B 專員...`, 'yellow');
        const privateMessage = {
            content: `測試私信 - ${new Date().toLocaleTimeString('zh-TW')}`,
            id: bUserId
        };
        
        log(`[A專員] 發送訊息到 /ws/privateMessage`, 'cyan');
        log(`[A專員] 訊息內容:`, 'cyan');
        console.log(JSON.stringify(privateMessage, null, 2));
        
        agentA.client.send('/ws/privateMessage', {}, JSON.stringify(privateMessage));
        log('[A專員] ✓ 私信已發送', 'green');
        
        // 6. 等待 B 專員接收訊息
        log('\n[步驟 6] 等待 B 專員接收訊息（最多 5 秒）...', 'yellow');
        let waitTime = 0;
        const maxWait = 5000;
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
            log('1. 用戶 ID 不正確（請檢查伺服器日誌中的實際用戶 ID）', 'yellow');
            log('2. B 專員未正確訂閱私信頻道', 'yellow');
            log('3. 伺服器端私信處理有問題', 'yellow');
            log('4. 網路延遲（可以增加等待時間）', 'yellow');
            log('\n建議：', 'cyan');
            log('- 檢查應用程式控制台的日誌，確認私信是否被處理', 'cyan');
            log('- 確認 B 專員的用戶 ID 是否正確（應該是第二個「登入用戶 ID」）', 'cyan');
            log('- 查看是否有「=== 私信發送調試 ===」的日誌', 'cyan');
            log('- 嘗試在瀏覽器中手動測試', 'cyan');
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

