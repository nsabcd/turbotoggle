package com.turbotoggle.sdk.store;

import com.turbotoggle.core.model.FlagConfigPayloadDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryFlagStore {
    private Map<String, FlagConfigPayloadDto> flagCache = new ConcurrentHashMap<>();

    public void updateAll(List<FlagConfigPayloadDto> flags){
        flagCache.clear();
        for(FlagConfigPayloadDto flag : flags){
            flagCache.put(flag.flagKey(), flag);
        }
    }

    public void putFlag(FlagConfigPayloadDto flag){
        flagCache.put(flag.flagKey(), flag);
    }

    public Optional<FlagConfigPayloadDto> getFlag(String flagKey){
        return Optional.ofNullable(flagCache.get(flagKey));
    }
}
