package com.dataflow.job.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class RedisLockService {

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    // Fallback local memory locks if Redis is unreachable / disabled
    private final ConcurrentMap<String, String> localLocks = new ConcurrentHashMap<>();

    public boolean tryLock(String lockKey, String lockValue, Duration expireTime) {
        if (redisTemplate != null) {
            try {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime);
                return Boolean.TRUE.equals(success);
            } catch (Exception e) {
                log.warn("Redis unreachable, falling back to local memory lock for key: {}", lockKey);
            }
        }
        return localLocks.putIfAbsent(lockKey, lockValue) == null;
    }

    public boolean unlock(String lockKey, String lockValue) {
        if (redisTemplate != null) {
            try {
                String currentValue = redisTemplate.opsForValue().get(lockKey);
                if (lockValue.equals(currentValue)) {
                    Boolean deleted = redisTemplate.delete(lockKey);
                    return Boolean.TRUE.equals(deleted);
                }
            } catch (Exception e) {
                log.warn("Redis error on unlock, cleaning up local memory fallback for key: {}", lockKey);
            }
        }
        return localLocks.remove(lockKey, lockValue);
    }
}
