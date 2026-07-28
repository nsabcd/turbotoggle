package com.turbotoggle.server.service;


import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.server.domain.entity.Environment;
import com.turbotoggle.server.repository.EnvironmentConfigRepository;
import com.turbotoggle.server.repository.EnvironmentRepository;
import com.turbotoggle.server.stub.InMemoryFlagCacheEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class FetchFlagServiceTest {
    @Mock
    private EnvironmentConfigRepository configRepository;

    @Mock
    private EnvironmentRepository environmentRepository;

    private InMemoryFlagCacheEngine cacheRepository;
    private FetchFlagService fetchFlagService;

    private final String sdkKey = "sdk_test_12345";
    private final String flagKey = "test-flag";

    @BeforeEach
    void setUp() {
        cacheRepository = new InMemoryFlagCacheEngine();
        fetchFlagService = new FetchFlagService(configRepository, environmentRepository, cacheRepository);
    }

    @Test
    @DisplayName("Should return cached flag configuration directly on cache HIT without querying DB")
    void shouldReturnFlagOnCacheHit(){
        FlagConfigPayloadDto expectedFlag = new FlagConfigPayloadDto(
                flagKey,
                "BOOLEAN",
                true,
                "true",
                List.of(),
                Map.of(),
                1L
        );
        cacheRepository.putFlag(sdkKey,expectedFlag);
        Optional<FlagConfigPayloadDto> result = fetchFlagService.getFlagConfig(sdkKey, flagKey);
        assertTrue(result.isPresent());
        assertEquals(expectedFlag, result.get());
        verifyNoInteractions(configRepository);
        verifyNoInteractions(environmentRepository);
    }

    @Test
    @DisplayName("Should fetch from DB and populate cache on cache MISS when lock is acquired")
    void shouldFetchFromDbAndPopulateCacheOnCacheMiss() {
        FlagConfigPayloadDto dbFlag = new FlagConfigPayloadDto(
                flagKey,
                "BOOLEAN",
                true,
                "false",
                List.of(),
                Map.of(),
                1L
        );
        when(configRepository.findPayloadDtoByFlagKeyAndSdkKey(flagKey, sdkKey))
                .thenReturn(Optional.of(dbFlag));

        Optional<FlagConfigPayloadDto> result = fetchFlagService.getFlagConfig(sdkKey, flagKey);

        assertTrue(result.isPresent());
        assertEquals(dbFlag, result.get());

        Optional<FlagConfigPayloadDto> cacheResult = cacheRepository.getFlag(sdkKey, flagKey);
        assertTrue(cacheResult.isPresent());
        assertEquals(dbFlag, cacheResult.get());
        verify(configRepository, times(1)).findPayloadDtoByFlagKeyAndSdkKey(flagKey, sdkKey);
    }

    @Test
    @DisplayName("Should resolve envKey mapping from cache or fallback to DB")
    void shouldResolveEnvKeyBySdkKey() {
        Environment environment = new Environment("production", sdkKey, "Production");
        when(environmentRepository.findBySdkKey(sdkKey)).thenReturn(Optional.of(environment));

        String envKey = fetchFlagService.getEnvKeyBySdkKey(sdkKey);

        assertEquals("production", envKey);
        assertEquals(Optional.of("production"), cacheRepository.getEnvKeyBySdkKey(sdkKey));
    }
}
