-- Lua 腳本：原子性地設置專員ID
-- 參數：
--   KEYS[1]: Redis key（例如 "agent:a:id" 或 "agent:b:id"）
--   ARGV[1]: 新的專員ID
--   ARGV[2]: 專員名稱（例如 "專員A" 或 "專員B"）
--   ARGV[3]: 當前時間戳（毫秒）

-- 如果key已存在，檢查是否為同一個ID
local existingId = redis.call('GET', KEYS[1])
local newId = ARGV[1]
local agentName = ARGV[2]
local timestamp = tonumber(ARGV[3])

-- 如果已存在且ID不同，則覆蓋（壓掉）
-- 如果已存在且ID相同，則更新時間戳
if existingId then
    if existingId ~= newId then
        -- ID不同，覆蓋舊的ID
        redis.call('SET', KEYS[1], newId)
        redis.call('HSET', KEYS[1] .. ':info', 'id', newId, 'name', agentName, 'timestamp', timestamp)
        redis.call('EXPIRE', KEYS[1], 86400) -- 24小時過期
        redis.call('EXPIRE', KEYS[1] .. ':info', 86400)
        -- 返回List格式：{status, oldId, newId}
        return {'replaced', existingId, newId}
    else
        -- ID相同，只更新時間戳
        redis.call('HSET', KEYS[1] .. ':info', 'timestamp', timestamp)
        redis.call('EXPIRE', KEYS[1], 86400)
        redis.call('EXPIRE', KEYS[1] .. ':info', 86400)
        -- 返回List格式：{status, id}
        return {'updated', newId}
    end
else
    -- 不存在，創建新的
    redis.call('SET', KEYS[1], newId)
    redis.call('HSET', KEYS[1] .. ':info', 'id', newId, 'name', agentName, 'timestamp', timestamp)
    redis.call('EXPIRE', KEYS[1], 86400)
    redis.call('EXPIRE', KEYS[1] .. ':info', 86400)
    -- 返回List格式：{status, id}
    return {'created', newId}
end
