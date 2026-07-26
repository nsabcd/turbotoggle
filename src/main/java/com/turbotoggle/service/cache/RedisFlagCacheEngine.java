package com.turbotoggle.service.cache;

import com.turbotoggle.domain.model.FlagConfigPayloadDto;
import com.turbotoggle.repository.Cache.FlagCacheRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RedisFlagCacheEngine implements FlagCacheRepository {
    private static final String FLAG_CACHE_PREFIX = "env_flags:";
    private static final String ENV_CACHE_PREFIX = "sdk_mapping:";
    private static final String CACHE_LOCK_PREFIX = "lock:";
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisFlagCacheEngine(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void putFlag(String sdkKey, FlagConfigPayloadDto flag) {
        String key = FLAG_CACHE_PREFIX+sdkKey;
        redisTemplate.opsForHash().put(key, flag.flagKey(), flag);
    }

    @Override
    public Optional<FlagConfigPayloadDto> getFlag(String sdkKey, String flagKey) {
        String key = FLAG_CACHE_PREFIX+sdkKey;
        Object val = redisTemplate.opsForHash().get(key, flagKey);
        if(val instanceof FlagConfigPayloadDto dto){
            return Optional.of(dto);
        }
        return Optional.empty();
    }

    @Override
    public List<FlagConfigPayloadDto> getAllFlags(String sdkKey) {
        String key = FLAG_CACHE_PREFIX+sdkKey;

        // Fetch all values from the Redis Hash (HVALS)
        List<Object> values = redisTemplate.opsForHash().values(key);

        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .filter(FlagConfigPayloadDto.class::isInstance)
                .map(FlagConfigPayloadDto.class::cast)
                .toList();
    }

    @Override
    public void evictEnvironment(String sdkKey) {
        redisTemplate.delete(FLAG_CACHE_PREFIX+sdkKey);
    }

    @Override
    public boolean acquireLock(String lockKey, String lockValue, long ttlSeconds) {
        // SET lockKey lockValue NX EX ttlSeconds
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(CACHE_LOCK_PREFIX + lockKey, lockValue, java.time.Duration.ofSeconds(ttlSeconds));
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void releaseLock(String lockKey, String lockValue) {
        String actualKey = CACHE_LOCK_PREFIX + lockKey;
        Object currentVal = redisTemplate.opsForValue().get(actualKey);
        if (lockValue.equals(currentVal)) {
            redisTemplate.delete(actualKey);
        }
    }

    @Override
    public Optional<String> getEnvKeyBySdkKey(String sdkKey) {
        Object val = redisTemplate.opsForValue().get(ENV_CACHE_PREFIX + sdkKey);
        return Optional.ofNullable((String) val);
    }

    @Override
    public void putEnvKeyMapping(String sdkKey, String envKey) {
        // Store in Redis with no expiration (or a long TTL)
        redisTemplate.opsForValue().set(ENV_CACHE_PREFIX + sdkKey, envKey);
    }

    @Override
    public void evictEnvKeyMapping(String sdkKey) {
        redisTemplate.delete(ENV_CACHE_PREFIX+sdkKey);
    }
}
