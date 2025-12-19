# Node.js readline 模組說明

## 什麼是 readline？

`readline` 是 Node.js 的**內建模組**，用於從**命令列（終端）讀取用戶輸入**。

## 程式碼解釋

```javascript
const readline = require('readline');
```
- **作用**：引入 Node.js 內建的 `readline` 模組
- **用途**：讓程式可以暫停執行，等待用戶在終端輸入文字

```javascript
const rl = readline.createInterface({
    input: process.stdin,   // 標準輸入（鍵盤輸入）
    output: process.stdout  // 標準輸出（螢幕輸出）
});
```
- **`createInterface()`**：建立一個讀取介面
- **`process.stdin`**：標準輸入流（用戶在終端輸入的內容）
- **`process.stdout`**：標準輸出流（程式顯示在終端的內容）

## 實際使用範例

```javascript
rl.question('請輸入您的名字: ', (answer) => {
    console.log(`您好，${answer}！`);
    rl.close(); // 關閉讀取介面
});
```

**執行效果：**
```
請輸入您的名字: 張三
您好，張三！
```

## 在我們的測試腳本中

```javascript
rl.question('\nB 專員的用戶 ID (32位字串，或按 Enter 跳過): ', async (bUserId) => {
    // bUserId 就是用戶輸入的內容
    // 如果用戶按 Enter，bUserId 就是空字串
});
```

**執行效果：**
```
B 專員的用戶 ID (32位字串，或按 Enter 跳過): abc123def456...
```
- 程式會**暫停**，等待用戶輸入
- 用戶輸入後按 Enter，輸入的內容會傳給回調函數
- 如果直接按 Enter，`bUserId` 就是空字串

## 為什麼需要這個？

因為我們需要**用戶手動輸入 B 專員的用戶 ID**（從伺服器日誌中獲取），所以需要暫停程式等待輸入。

## 替代方案

如果不想手動輸入，可以：
1. 從環境變數讀取：`process.env.B_USER_ID`
2. 從檔案讀取：讀取日誌檔案並解析
3. 從 API 獲取：建立一個 API 端點來獲取連接的用戶列表



