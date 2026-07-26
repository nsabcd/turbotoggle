package com.turbotoggle.domain.event;

import com.turbotoggle.domain.model.FlagConfigPayloadDto;

public record FlagConfigUpdatedEvent(
        String sdkKey,
        FlagConfigPayloadDto updatedFlag
) {}
