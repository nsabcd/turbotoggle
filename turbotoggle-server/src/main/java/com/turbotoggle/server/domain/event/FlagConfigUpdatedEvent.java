package com.turbotoggle.server.domain.event;

import com.turbotoggle.core.model.FlagConfigPayloadDto;

public record FlagConfigUpdatedEvent(
        String sdkKey,
        String flagKey,
        Long version,
        FlagConfigPayloadDto updatedFlag
) {}
