package com.turbotoggle.service.cache;

import com.turbotoggle.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.domain.model.FlagConfigPayloadDto;

public interface FlagMutationPublisher {
    void publishMutation(FlagConfigUpdatedEvent event);
}