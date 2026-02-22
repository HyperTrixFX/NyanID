package moe.koseirin.nyanruaineo.utils.RedisUtils;

/*
 * @author KoseiRin_
 * awa
 */

import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RedisHash
public class RedisService {


    private final RedisTemplate<String, Object> redisTemplate;

/**
 * Constructor for RedisService class that initializes with a RedisTemplate.
 *
 * @param redisTemplate The RedisTemplate to be used for Redis operations.
 *                      Must not be null.
 * @throws IllegalArgumentException if redisTemplate is null
 */
public RedisService(RedisTemplate<String, Object> redisTemplate) {
    if (redisTemplate == null) {
        throw new IllegalArgumentException("RedisTemplate cannot be null");
    }
    this.redisTemplate = redisTemplate;
}




    /**
     * 设置一个值
     * @param key     键
     * @param   value 值
     */
    public void setValue(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }




    /**
     * 创建一个值
     * @param key     键
     * @return  value 值
     */
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }




    /**
     * 创建一个时间性值
     * @param key     键
     * @param value   值
     * @param timeout 时间
     * @param timeUnit 时间单位
     */
    public void setValueWithExpiration(String key, Object value, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }



    /**
     * 删除
     * @param key     键
     */
    public void deleteValue(String key) {
        redisTemplate.delete(key);
    }



    /**
     * 检查键是否存在
     * @param key 键
 85  *      * @return true如果键存在，false如果键不存在
     */
    public boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }





    /**
     * 设置键的过期时间
     * @param key 键
     * @param timeout 过期时间
     * @param timeUnit 时间单位
     * @return true如果设置成功，false如果键不存在或设置失败
     */
    public boolean expire(String key, long timeout, TimeUnit timeUnit) {
        return redisTemplate.expire(key, timeout, timeUnit);
    }




    /**
     * 获取键的剩余过期时间
     * @param key 键
     * @param timeUnit 时间单位
     * @return 剩余过期时间，-1表示键存在但没有设置过期时间，-2表示键不存在
     */
    public long getExpire(String key, TimeUnit timeUnit) {
        return redisTemplate.getExpire(key, timeUnit);
    }





    /**
     * 批量删除键
     * @param keys 键集合
     * @return 删除的键数量
     */
    public long deleteValues(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count != null ? count : 0;
    }



    /**
     * 批量设置值
     * @param map 键值对映射
     */
    public void setValues(Map<String, Object> map) {
        redisTemplate.opsForValue().multiSet(map);
    }


    /**
     * 获取匹配模式的所有键
     * @param pattern 匹配模式
     * @return 匹配的键集合
     */
    public Set<String> getKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }



    public int getAll() {
        return Objects.requireNonNull(redisTemplate.keys("*")).toString().length();
    }




/**
 * Attempts to acquire a distributed lock with the given key and value.
 *
 * @param key The key of the lock to acquire
 * @param value The value to set for the lock (used for lock ownership verification)
 * @param waitTime Maximum time to wait for the lock in seconds
 * @param expireTime Time after which the lock will automatically expire in seconds
 * @return true if the lock was successfully acquired, false if the acquisition timed out
 */
    public boolean tryLock(String key, String value, long waitTime, long expireTime) {
        long startTime = System.currentTimeMillis();
        long timeout = waitTime * 1000;

        while (true) {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(key, value, expireTime, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(result)) {
                return true;
            }
            if (System.currentTimeMillis() - startTime > timeout) {
                return false;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }



/**
 * Unlocks a resource by removing the lock from Redis if the provided key matches the stored value.
 * This method uses a Lua script to ensure atomic operation and prevent race conditions.
 *
 * @param key The key identifying the lock to be released
 * @param value The expected value of the lock, ensuring only the lock holder can release it
 */
    public void unlock(String key, String value) {
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(key), value);
    }

    }





