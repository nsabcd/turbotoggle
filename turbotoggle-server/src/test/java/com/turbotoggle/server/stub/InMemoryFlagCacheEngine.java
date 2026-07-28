package com.turbotoggle.server.stub;

import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.server.repository.Cache.FlagCacheRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFlagCacheEngine implements FlagCacheRepository {

    private final Map<String, Map<String, FlagConfigPayloadDto>> store = new ConcurrentHashMap<>();
    private final Map<String, String> envKeyMappings = new ConcurrentHashMap<>();
    private final Set<String> locks = ConcurrentHashMap.newKeySet();

    @Override
    public void putFlag(String sdkKey, FlagConfigPayloadDto flag) {
        store.computeIfAbsent(sdkKey, k -> new ConcurrentHashMap<>())
                .put(flag.flagKey(), flag);
    }

    @Override
    public Optional<FlagConfigPayloadDto> getFlag(String sdkKey, String flagKey) {
        Map<String, FlagConfigPayloadDto> envMap = store.get(sdkKey);
        return envMap == null ? Optional.empty() : Optional.ofNullable(envMap.get(flagKey));
    }

    @Override
    public void evictEnvironment(String sdkKey) {
        store.remove(sdkKey);
    }

    @Override
    public Optional<String> getEnvKeyBySdkKey(String sdkKey) {
        return Optional.ofNullable(envKeyMappings.get(sdkKey));
    }

    @Override
    public void putEnvKeyMapping(String sdkKey, String envKey) {
        envKeyMappings.put(sdkKey, envKey);
    }

    @Override
    public void evictEnvKeyMapping(String sdkKey) {
        envKeyMappings.remove(sdkKey);
    }

    @Override
    public List<FlagConfigPayloadDto> getAllFlags(String sdkKey) {
        Map<String, FlagConfigPayloadDto> envMap = store.get(sdkKey);
        return envMap == null ? Collections.emptyList() : new ArrayList<>(envMap.values());
    }

    @Override
    public boolean acquireLock(String lockKey, String lockValue, long ttlSeconds) {
        return locks.add(lockKey);
    }

    @Override
    public void releaseLock(String lockKey, String lockValue) {
        locks.remove(lockKey);
    }

    @Override
    public List<FlagConfigPayloadDto> getFlagsBulk(String sdkKey, List<String> flagKeys) {
        Map<String, FlagConfigPayloadDto> envMap = store.get(sdkKey);
        if (envMap == null) return Collections.emptyList();

        List<FlagConfigPayloadDto> results = new ArrayList<>();
        for (String key : flagKeys) {
            if (envMap.containsKey(key)) {
                results.add(envMap.get(key));
            }
        }
        return results;
    }
}