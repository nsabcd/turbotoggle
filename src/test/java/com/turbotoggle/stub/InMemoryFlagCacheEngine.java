package com.turbotoggle.stub;

import com.turbotoggle.domain.model.FlagConfigPayloadDto;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFlagCacheEngine implements FlagCacheRepository {

    private final Map<String, Map<String, FlagConfigPayloadDto>> store = new ConcurrentHashMap<>();

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
}