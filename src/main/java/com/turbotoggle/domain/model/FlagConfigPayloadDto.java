package com.turbotoggle.domain.model;

import java.util.List;
import java.util.Map;

public record FlagConfigPayloadDto(
        String flagKey,
        String flagType,
        boolean enabled,
        String defaultVariation,
        List<Object> rules,
        Map<String, String> individualTargets,
        long version
) {}