ㄇ# PowerShell 哈希表（Hashtable）說明

## 什麼是 `@{ message = ... }`？

`@{ }` 是 PowerShell 的**哈希表（Hashtable）**語法，用來建立一個鍵值對（key-value）的物件。

## 語法說明

### 基本語法
```powershell
@{ key1 = value1; key2 = value2 }
```

### 在腳本中的使用
```powershell
$body = @{ message = "測試訊息 $(Get-Date -Format 'HH:mm:ss')" } | ConvertTo-Json
```

## 詳細解釋

### 1. `@{ }` - 哈希表語法
- **`@`**: PowerShell 的哈希表前綴符號
- **`{ }`**: 包圍鍵值對的大括號
- **作用**: 建立一個類似字典或物件的資料結構

### 2. `message = "測試訊息..."` - 鍵值對
- **`message`**: 鍵（key），類似物件的屬性名稱
- **`=`**: 賦值運算符
- **`"測試訊息..."`**: 值（value），這個屬性的值

### 3. `| ConvertTo-Json` - 管道轉換
- **`|`**: PowerShell 管道運算符，將左邊的結果傳給右邊的命令
- **`ConvertTo-Json`**: 將 PowerShell 物件轉換為 JSON 字串

## 完整流程

```powershell
# 步驟 1: 建立哈希表
@{ message = "測試訊息 17:30:45" }
# 結果: PowerShell 哈希表物件

# 步驟 2: 轉換為 JSON
@{ message = "測試訊息 17:30:45" } | ConvertTo-Json
# 結果: {"message":"測試訊息 17:30:45"}

# 步驟 3: 儲存到變數
$body = @{ message = "測試訊息 17:30:45" } | ConvertTo-Json
# $body 現在包含: {"message":"測試訊息 17:30:45"}
```

## 實際範例

### 範例 1: 單一鍵值對
```powershell
# 建立哈希表
$data = @{ message = "Hello, World!" }

# 存取值
$data.message        # 輸出: Hello, World!
$data["message"]     # 輸出: Hello, World!
```

### 範例 2: 多個鍵值對
```powershell
# 建立多個鍵值對的哈希表
$user = @{
    name = "張三"
    age = 25
    city = "台北"
}

# 存取值
$user.name    # 輸出: 張三
$user.age     # 輸出: 25
$user.city    # 輸出: 台北
```

### 範例 3: 轉換為 JSON（腳本中的用法）
```powershell
# 建立哈希表
$body = @{ 
    message = "測試訊息 $(Get-Date -Format 'HH:mm:ss')" 
} | ConvertTo-Json

# $body 的內容:
# {"message":"測試訊息 17:30:45"}
```

### 範例 4: 用於 HTTP 請求
```powershell
# 建立請求體
$body = @{ 
    message = "Hello, World!" 
} | ConvertTo-Json

# 發送 HTTP POST 請求
Invoke-WebRequest -Uri "http://localhost:8080/sendMessage" `
    -Method POST `
    -Body $body `
    -ContentType "application/json"
```

## 與其他語言的對比

### JavaScript 物件
```javascript
// JavaScript
const data = { message: "Hello, World!" };
```

### PowerShell 哈希表
```powershell
# PowerShell
$data = @{ message = "Hello, World!" }
```

### Python 字典
```python
# Python
data = { "message": "Hello, World!" }
```

## 常見用法

### 1. 建立簡單物件
```powershell
$person = @{
    name = "李四"
    age = 30
}
```

### 2. 動態建立
```powershell
$key = "message"
$value = "Hello"
$data = @{ $key = $value }
```

### 3. 巢狀結構
```powershell
$complex = @{
    user = @{
        name = "王五"
        address = @{
            city = "台中"
            zip = "400"
        }
    }
}
```

### 4. 轉換為 JSON
```powershell
$json = @{ message = "Hello" } | ConvertTo-Json
# 結果: {"message":"Hello"}
```

## 在腳本中的實際應用

### 原始代碼
```powershell
$body = @{ message = "測試訊息 $(Get-Date -Format 'HH:mm:ss')" } | ConvertTo-Json
```

### 等價寫法 1: 分步驟
```powershell
# 步驟 1: 建立哈希表
$hashtable = @{ message = "測試訊息 $(Get-Date -Format 'HH:mm:ss')" }

# 步驟 2: 轉換為 JSON
$body = $hashtable | ConvertTo-Json
```

### 等價寫法 2: 使用變數
```powershell
# 先建立訊息內容
$messageContent = "測試訊息 $(Get-Date -Format 'HH:mm:ss')"

# 建立哈希表
$body = @{ message = $messageContent } | ConvertTo-Json
```

### 等價寫法 3: 多個參數
```powershell
$body = @{ 
    message = "測試訊息"
    timestamp = Get-Date -Format 'HH:mm:ss'
    type = "test"
} | ConvertTo-Json

# 結果: {"message":"測試訊息","timestamp":"17:30:45","type":"test"}
```

## 重要注意事項

### 1. 鍵值對分隔
```powershell
# 正確: 使用分號或換行分隔多個鍵值對
@{ key1 = "value1"; key2 = "value2" }
@{ 
    key1 = "value1"
    key2 = "value2"
}

# 錯誤: 不能只用逗號
@{ key1 = "value1", key2 = "value2" }  # ❌ 錯誤
```

### 2. 字串插值
```powershell
# 使用 $() 進行字串插值
@{ message = "時間: $(Get-Date)" }

# 或使用變數
$time = Get-Date
@{ message = "時間: $time" }
```

### 3. JSON 轉換
```powershell
# ConvertTo-Json 會自動處理轉義
@{ message = "Hello \"World\"" } | ConvertTo-Json
# 結果: {"message":"Hello \"World\""}
```

## 總結

- **`@{ }`**: PowerShell 哈希表語法，建立鍵值對物件
- **`message = "..."`**: 定義一個名為 `message` 的鍵，值為字串
- **`| ConvertTo-Json`**: 將哈希表轉換為 JSON 字串
- **用途**: 建立 HTTP 請求的 JSON 請求體

## 在您的腳本中

```powershell
$body = @{ message = "測試訊息 $(Get-Date -Format 'HH:mm:ss')" } | ConvertTo-Json
```

這行程式碼的作用：
1. 建立一個哈希表，包含 `message` 鍵和動態時間戳記的值
2. 將哈希表轉換為 JSON 字串
3. 儲存到 `$body` 變數中，用於 HTTP POST 請求

最終 `$body` 的內容類似：
```json
{"message":"測試訊息 17:30:45"}
```



