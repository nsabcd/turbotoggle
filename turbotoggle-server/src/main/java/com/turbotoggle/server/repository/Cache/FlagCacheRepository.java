package com.turbotoggle.server.repository.Cache;

import com.turbotoggle.core.model.FlagConfigPayloadDto;

import java.util.List;
import java.util.Optional;

public interface FlagCacheRepository {

    /**
     * Stores or updates a single flag in the environment cache.
     */
    void putFlag(String sdkKey, FlagConfigPayloadDto flag);

    /**
     * Retrieves a single cached flag configuration.
     */
    Optional<FlagConfigPayloadDto> getFlag(String sdkKey, String flagKey);

    /**
     * Evicts the entire cache bucket for an environment (e.g., on bulk resets).
     */
    void evictEnvironment(String sdkKey);

    Optional<String> getEnvKeyBySdkKey(String sdkKey);

    void putEnvKeyMapping(String sdkKey, String envKey);

    void evictEnvKeyMapping(String sdkKey);

    List<FlagConfigPayloadDto> getAllFlags(String sdkKey);
    /**
     * Attempts to acquire a distributed lock with a TTL.
     */
    boolean acquireLock(String lockKey, String lockValue, long ttlSeconds);

    /**
     * Releases the distributed lock if the value matches.
     */
    void releaseLock(String lockKey, String lockValue);

    List<FlagConfigPayloadDto> getFlagsBulk(String sdkKey, List<String> flagKeys);
}