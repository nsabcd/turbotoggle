package com.turbotoggle.server.service.cache;

import com.turbotoggle.server.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.server.repository.Cache.FlagCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

public class CacheInvalidationListener {
    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationListener.class);
    private final FlagCacheRepository cacheRepository;
    private final FlagMutationPublisher mutationPublisher;

    public CacheInvalidationListener(FlagCacheRepository cacheRepository, FlagMutationPublisher mutationPublisher) {
        this.cacheRepository = cacheRepository;
        this.mutationPublisher = mutationPublisher;
    }

    @Async
    @EventListener
    public void handleFlagMutatation(FlagConfigUpdatedEvent event){
        log.info("Processing flag muation event for flag [{}] in env [{}]", event.updatedFlag().flagKey(), event.sdkKey());
        try{
            cacheRepository.putFlag(event.sdkKey(), event.updatedFlag());
            mutationPublisher.publishMutation(event);
        }catch (Exception e){
            log.error("Failed to process cache Invalidation for flag [{}]", event.updatedFlag().flagKey(), e);
        }
    }
}
