-- Lua 腳本：保存聊天訊息到 Redis
-- 使用 Sorted Set 存儲訊息，以時間戳作為分數，確保訊息按時間排序
-- Key 格式：chat:messages:public 或 chat:messages:private:{userId}

-- 參數說明：
-- KEYS[1]: 訊息列表的 key（如：chat:messages:public）
-- ARGV[1]: 訊息 JSON 字串
-- ARGV[2]: 時間戳（毫秒）
-- ARGV[3]: 最大訊息數量（超過此數量會刪除最舊的訊息）

-- 返回值：保存的訊息數量

-- 1. 將訊息添加到 Sorted Set（使用時間戳作為分數）
redis.call('ZADD', KEYS[1], ARGV[2], ARGV[1])

-- 2. 獲取當前訊息數量
local count = redis.call('ZCARD', KEYS[1])

-- 3. 如果超過最大數量，刪除最舊的訊息
local maxMessages = tonumber(ARGV[3])
if maxMessages > 0 and count > maxMessages then
    -- 刪除最舊的訊息（分數最小的）
    local removed = redis.call('ZREMRANGEBYRANK', KEYS[1], 0, count - maxMessages - 1)
    count = count - removed
end

-- 4. 設置過期時間（30天）
redis.call('EXPIRE', KEYS[1], 2592000)

-- 返回當前訊息數量
return count









