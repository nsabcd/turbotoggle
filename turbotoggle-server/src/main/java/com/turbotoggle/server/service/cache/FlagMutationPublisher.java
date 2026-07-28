package com.turbotoggle.server.service.cache;

import com.turbotoggle.server.domain.event.FlagConfigUpdatedEvent;

public interface FlagMutationPublisher {
    void publishMutation(FlagConfigUpdatedEvent event);
}