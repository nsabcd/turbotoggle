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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    @DisplayName("Should fetch from DB and populate cache on cache MISS when lock is acquired")
    void shouldFetchFromDbAndPopulateCacheOnCacheMiss_Threaded() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
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

        Future<Optional<FlagConfigPayloadDto>> future1 = executor.submit(() -> {
            return fetchFlagService.getFlagConfig(sdkKey, flagKey);
        });
        Future<Optional<FlagConfigPayloadDto>> future2 = executor.submit(() -> {
            return fetchFlagService.getFlagConfig(sdkKey, flagKey);
        });

        try {
            Optional<FlagConfigPayloadDto> result1 = future1.get();
            Optional<FlagConfigPayloadDto> result2 = future2.get();

            assertTrue(result1.isPresent());
            assertEquals(dbFlag, result1.get());
        }catch (Exception e){

        }
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


    @Test
    void getEnvKeyBySdkKey_shouldThrow_whenSdkKeyNotFound() {
        when(environmentRepository.findBySdkKey("invalid-sdk"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> fetchFlagService.getEnvKeyBySdkKey("invalid-sdk"));
    }

    @Test
    void getAllFlagsForSdk_shouldReturnFlagPayloads_whenSdkKeyIsValid() {
        // Arrange
        String sdkKey = "valid-sdk-key-123";
        String envKey = "env-1";

        Environment env = new Environment(
                envKey,
                sdkKey,
                "name"
        );
        FlagConfigPayloadDto mockConfig1 = new FlagConfigPayloadDto(
                "flag-1",
                "BOOLEAN",
                true,
                "false",
                List.of(),
                Map.of(),
                1L
        );
        FlagConfigPayloadDto mockConfig2 = new FlagConfigPayloadDto(
                "flag-2",
                "INTEGER",
                false,
                "false",
                List.of(),
                Map.of(),
                1L
        );

        when(environmentRepository.findBySdkKey(sdkKey))
                .thenReturn(Optional.of(env));
        when(configRepository.findAllFlagPayloadDtosBySdkKey(sdkKey))
                .thenReturn(List.of(mockConfig1, mockConfig2));

        // Act
        List<FlagConfigPayloadDto> results = fetchFlagService.getAllFlagsForSdk(sdkKey);

        // Assert
        assertThat(results).isNotNull().hasSize(2);

        FlagConfigPayloadDto flag = results.get(0);
        assertThat(flag.flagKey()).isEqualTo("flag-1");
        assertThat(flag.flagType()).isEqualTo("BOOLEAN");
        assertThat(flag.enabled()).isTrue();

        flag = results.get(1);
        assertThat(flag.flagKey()).isEqualTo("flag-2");
        assertThat(flag.flagType()).isEqualTo("INTEGER");
        assertThat(flag.enabled()).isFalse();
    }

    @Test
    void getAllFlagsForSdk_shouldReturnFlagPayloads_whenSdkKeyIsValid_Blocked() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // Arrange
        String sdkKey = "valid-sdk-key-123";
        String envKey = "env-1";

        Environment env = new Environment(
                envKey,
                sdkKey,
                "name"
        );
        FlagConfigPayloadDto mockConfig1 = new FlagConfigPayloadDto(
                "flag-1",
                "BOOLEAN",
                true,
                "false",
                List.of(),
                Map.of(),
                1L
        );
        FlagConfigPayloadDto mockConfig2 = new FlagConfigPayloadDto(
                "flag-2",
                "INTEGER",
                false,
                "false",
                List.of(),
                Map.of(),
                1L
        );

        when(environmentRepository.findBySdkKey(sdkKey))
                .thenReturn(Optional.of(env));
        when(configRepository.findAllFlagPayloadDtosBySdkKey(sdkKey))
                .thenReturn(findAllFlagPayloadDtosBySdkKey_10SecBlock(List.of(mockConfig1, mockConfig2)));

        // Act
        Future<List<FlagConfigPayloadDto>>  result1 = executor.submit(() -> {
            return fetchFlagService.getAllFlagsForSdk(sdkKey);
        });
        Future<List<FlagConfigPayloadDto>>  result2 = executor.submit(() -> {
            return fetchFlagService.getAllFlagsForSdk(sdkKey);
        });
        try{
            List<FlagConfigPayloadDto> results1 = result1.get();
            List<FlagConfigPayloadDto> results2 = result2.get();

            // Assert
            assertThat(results1).isNotNull().hasSize(2);

            FlagConfigPayloadDto flag = results1.get(0);
            assertThat(flag.flagKey()).isEqualTo("flag-1");
            assertThat(flag.flagType()).isEqualTo("BOOLEAN");
            assertThat(flag.enabled()).isTrue();

            flag = results1.get(1);
            assertThat(flag.flagKey()).isEqualTo("flag-2");
            assertThat(flag.flagType()).isEqualTo("INTEGER");
            assertThat(flag.enabled()).isFalse();

            // Assert
            assertThat(results1).isNotNull().hasSize(2);

            flag = results1.get(0);
            assertThat(flag.flagKey()).isEqualTo("flag-1");
            assertThat(flag.flagType()).isEqualTo("BOOLEAN");
            assertThat(flag.enabled()).isTrue();

            flag = results1.get(1);
            assertThat(flag.flagKey()).isEqualTo("flag-2");
            assertThat(flag.flagType()).isEqualTo("INTEGER");
            assertThat(flag.enabled()).isFalse();
        }catch(Exception e){

        }

    }

    @Test
    void getEnvKeyBySdkKey_shouldReturnEnvKey_whenSdkKeyIsValid_Blocked() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        // Arrange
        String sdkKey = "valid-sdk-key-123";
        String envKey = "env-1";

        Environment env = new Environment(
                envKey,
                sdkKey,
                "name"
        );

        when(environmentRepository.findBySdkKey(sdkKey))
                .thenReturn(getEnvKeyBySdkKey_10SecBlock(Optional.of(env)));

        // Act
        Future<String>  result1 = executor.submit(() -> {
            return fetchFlagService.getEnvKeyBySdkKey(sdkKey);
        });
        Future<String>  result2 = executor.submit(() -> {
            return fetchFlagService.getEnvKeyBySdkKey(sdkKey);
        });
        try{
            String str1 = result1.get();
            String str2 = result2.get();

            assertThat(str1).isEqualTo("env-1");
            assertThat(str2).isEqualTo("env-1");
        }catch(Exception e){

        }

    }

    private List<FlagConfigPayloadDto> findAllFlagPayloadDtosBySdkKey_10SecBlock(List<FlagConfigPayloadDto> list){
        try {
            Thread.sleep(2000);
        }catch (InterruptedException e){

        }finally {
            return list;
        }

    }
    private Optional<Environment> getEnvKeyBySdkKey_10SecBlock(Optional<Environment> env){
        try {
            Thread.sleep(2000);
        }catch (InterruptedException e){

        }finally {
            return env;
        }

    }
}
