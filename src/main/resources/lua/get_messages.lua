-- Lua 腳本：從 Redis 獲取聊天訊息
-- 使用 Sorted Set 獲取指定範圍的訊息

-- 參數說明：
-- KEYS[1]: 訊息列表的 key（如：chat:messages:public）
-- ARGV[1]: 開始索引（從 0 開始，負數表示從末尾開始）
-- ARGV[2]: 結束索引（-1 表示最後一個）
-- ARGV[3]: 是否反轉順序（1=反轉，0=不反轉）

-- 返回值：訊息陣列

-- 1. 獲取訊息範圍
local reverse = tonumber(ARGV[3]) == 1
local messages

if reverse then
    -- 反轉順序（最新的在前）
    messages = redis.call('ZREVRANGE', KEYS[1], ARGV[1], ARGV[2])
else
    -- 正常順序（最舊的在前）
    messages = redis.call('ZRANGE', KEYS[1], ARGV[1], ARGV[2])
end

-- 2. 返回訊息陣列
return messages









