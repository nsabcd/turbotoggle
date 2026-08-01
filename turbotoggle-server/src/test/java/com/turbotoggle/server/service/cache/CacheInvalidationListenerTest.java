package com.turbotoggle.server.service.cache;

import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.server.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.server.repository.Cache.FlagCacheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CacheInvalidationListenerTest {
    @Mock
    private FlagCacheRepository flagCacheRepository;

    @Mock
    private FlagMutationPublisher flagMutationPublisher;

    @InjectMocks
    private CacheInvalidationListener listener;

    @Test
    void handleFlagMutation_shouldEvictCacheAndPublish() {
        String flagKey = "flag-1";
        FlagConfigPayloadDto expectedFlag = new FlagConfigPayloadDto(
                flagKey,
                "BOOLEAN",
                true,
                "true",
                List.of(),
                Map.of(),
                1L
        );
        FlagConfigUpdatedEvent event = new FlagConfigUpdatedEvent("env1", flagKey,1L, expectedFlag);

        listener.handleFlagMutatation(event);

        verify(flagCacheRepository).putFlag("env1", expectedFlag);
        verify(flagMutationPublisher).publishMutation(event);
    }


}
