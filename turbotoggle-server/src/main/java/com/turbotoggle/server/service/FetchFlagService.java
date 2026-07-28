package com.turbotoggle.server.service;

import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.server.domain.entity.Environment;
import com.turbotoggle.server.repository.Cache.FlagCacheRepository;
import com.turbotoggle.server.repository.EnvironmentConfigRepository;
import com.turbotoggle.server.repository.EnvironmentRepository;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;


@Service
@Transactional(readOnly = true)
public class FetchFlagService {
    private static final int LOCK_DURATION_SECONDS = 3;
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_WAIT_MS = 50;
    private static final Logger log = LoggerFactory.getLogger(FetchFlagService.class);

    private final EnvironmentConfigRepository configRepository;
    private final EnvironmentRepository envRepository;
    private final FlagCacheRepository cacheRepository;

    public FetchFlagService(EnvironmentConfigRepository configRepository, EnvironmentRepository envRepository, FlagCacheRepository cacheRepository) {
        this.configRepository = configRepository;
        this.cacheRepository = cacheRepository;
        this.envRepository=envRepository;
    }

    /**
     * Resolves single flag configuration: Redis First -> DB Fallback -> Cache Fill
     */
    public Optional<FlagConfigPayloadDto> getFlagConfig(String sdkKey, String flagKey){
        String lockKey = sdkKey+":"+flagKey;
        for(int attempt=0;attempt<MAX_RETRIES;attempt++){
            Optional<FlagConfigPayloadDto> cachedFlag = cacheRepository.getFlag(sdkKey, flagKey);
            if(cachedFlag.isPresent()){
                log.debug("Cache hit for flag [{}] in env [{}].", flagKey, sdkKey);
                return cachedFlag;
            }
            log.info("Cache miss for flag [{}] in env [{}]. Fetching from DB.",flagKey, sdkKey);
            String lockValue = UUID.randomUUID().toString();
            boolean acquired = cacheRepository.acquireLock(lockKey, lockValue, LOCK_DURATION_SECONDS);
            if(acquired){
                try{
                    cachedFlag = cacheRepository.getFlag(sdkKey, flagKey);
                    if(cachedFlag.isPresent()){
                        log.debug("Cache hit for flag after lock [{}] in env [{}].", flagKey, sdkKey);
                        return cachedFlag;
                    }
                    log.info("Cache miss for flag [{}] in env [{}] after lock. Fetching from DB.",flagKey, sdkKey);
                    Optional<FlagConfigPayloadDto> dbFlag  = configRepository.findPayloadDtoByFlagKeyAndSdkKey(flagKey, sdkKey);
                    if(dbFlag.isEmpty()){
                        log.error("DB miss for flag [{}] in env [{}]. Fetching from DB.",flagKey, sdkKey);
                        return Optional.empty();
                    }
                    dbFlag.ifPresent(flag -> cacheRepository.putFlag(sdkKey, flag));
                    return dbFlag;
                }finally{
                    cacheRepository.releaseLock(lockKey, lockValue);
                }

            }
            //Failed to acquire the lock
            //sleep with backoff
            try{
                long sleepTime = INITIAL_WAIT_MS * (1L << attempt) + (long)(Math.random()*20);
                log.debug("Lock busy for env [{}] and flag [{}]. Waiting {} ms (attempt {}/{})",
                        sdkKey, flagKey, sleepTime, attempt + 1, MAX_RETRIES);
                Thread.sleep(sleepTime);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted while waiting for cache warmup", e);
            }
        }
        log.warn("Exhausted retries waiting for lock on flag [{}] in env [{}]. Safety DB fallback.", flagKey, sdkKey);
        return configRepository.findPayloadDtoByFlagKeyAndSdkKey(flagKey, sdkKey);
    }


    /**
     * Resolves the environment key from an SDK Key.
     */
    public String getEnvKeyBySdkKey(String sdkKey) {// Short wait since mapping queries are very fast
        String lockKey = "mapping:" + sdkKey;

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            // 1. Fast Path: Read mapping from Redis
            Optional<String> cachedEnvKey = cacheRepository.getEnvKeyBySdkKey(sdkKey);
            if (cachedEnvKey.isPresent()) {
                return cachedEnvKey.get();
            }

            // 2. Try acquiring a distributed lock specifically for this SDK key mapping
            String lockValue = java.util.UUID.randomUUID().toString();
            boolean acquired = cacheRepository.acquireLock(lockKey, lockValue, 3); // 3-second lock

            if (acquired) {
                try {
                    // Double-check: Did another thread cache it while we waited for the lock?
                    cachedEnvKey = cacheRepository.getEnvKeyBySdkKey(sdkKey);
                    if (cachedEnvKey.isPresent()) {
                        return cachedEnvKey.get();
                    }

                    // 3. Lock Acquired: Fetch from DB
                    log.info("Cache MISS with Lock for SDK mapping [{}]. Fetching from DB...", sdkKey);
                    String envKey = envRepository.findBySdkKey(sdkKey)
                            .map(Environment::getEnvKey)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid SDK Key: " + sdkKey));

                    // 4. Populate Cache and return
                    cacheRepository.putEnvKeyMapping(sdkKey, envKey);
                    return envKey;
                } finally {
                    cacheRepository.releaseLock(lockKey, lockValue);
                }
            }

            // 5. Failed to acquire lock — another thread is querying DB. Backoff + Jitter.
            try {
                long sleepTime = INITIAL_WAIT_MS * (1L << attempt) + (long) (Math.random() * 10);
                log.debug("Lock busy for SDK mapping [{}]. Waiting {} ms (attempt {}/{})",
                        sdkKey, sleepTime, attempt + 1, MAX_RETRIES);
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for SDK mapping cache lock", e);
            }
        }

        // 6. Safety Valve Fallback: Direct DB query if retries exhausted
        log.warn("Exhausted retries waiting for lock on SDK mapping [{}]. Safety DB fallback.", sdkKey);
        return envRepository.findBySdkKey(sdkKey)
                .map(Environment::getEnvKey)
                .orElseThrow(() -> new IllegalArgumentException("Invalid SDK Key: " + sdkKey));
    }

    /**
     * Fetches all flag payloads for SDK initialization.
     */
    public List<FlagConfigPayloadDto> getAllFlagsForSdk(String sdkKey) {
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++){
            List<FlagConfigPayloadDto> cachedFlags = cacheRepository.getAllFlags(sdkKey);
            if(!cachedFlags.isEmpty()){
                log.debug("Cache hit for env [{}], [{}] flags found.", sdkKey, cachedFlags.size());
                return cachedFlags;
            }
            log.debug("Cache miss for env [{}], Fetching from DB.", sdkKey);
            String lockValue = java.util.UUID.randomUUID().toString();
            boolean acquired = cacheRepository.acquireLock(sdkKey, lockValue, LOCK_DURATION_SECONDS);

            if(acquired){
                try{
                    cachedFlags = cacheRepository.getAllFlags(sdkKey);
                    if(!cachedFlags.isEmpty()){
                        log.debug("Cache hit for env after lock [{}], [{}] flags found.", sdkKey, cachedFlags.size());
                        return cachedFlags;
                    }
                    // Validate SDK key exists
                    getEnvKeyBySdkKey(sdkKey);
                    List<FlagConfigPayloadDto> dbFlags = configRepository.findAllFlagPayloadDtosBySdkKey(sdkKey);
                    for(FlagConfigPayloadDto flag : dbFlags){
                        cacheRepository.putFlag(sdkKey, flag);
                    }
                    return dbFlags;
                }finally {
                    cacheRepository.releaseLock(sdkKey, lockValue);
                }
            }

            //Failed to acquire the lock
            //sleep with backoff
            try{
                long sleepTime = INITIAL_WAIT_MS * (1L << attempt) + (long)(Math.random()*20);
                log.debug("Lock busy for env [{}]. Waiting {} ms (attempt {}/{})",
                        sdkKey, sleepTime, attempt + 1, MAX_RETRIES);
                Thread.sleep(sleepTime);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted while waiting for cache warmup", e);
            }
        }
        log.warn("Exhauster retires waititng for lock on env [{}], executing safety DB fallback.", sdkKey);
        return configRepository.findAllFlagPayloadDtosBySdkKey(sdkKey);
    }

    public List<FlagConfigPayloadDto> getFlagConfigs(String sdkKey, List<String> flagKeys) {
        if (flagKeys == null || flagKeys.isEmpty()) {
            return Collections.emptyList();
        }

        return flagKeys.stream()
                .map(flagKey -> getFlagConfig(sdkKey, flagKey)) // Returns Optional<FlagConfigPayloadDto>
                .flatMap(Optional::stream)                     // Filters out empty Optionals cleanly (Java 9+)
                .toList();                                     // Collects to unmodifiable List (Java 16+)
    }


}
