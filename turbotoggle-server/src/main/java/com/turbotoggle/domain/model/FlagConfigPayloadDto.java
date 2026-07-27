package com.turbotoggle.domain.model;

import com.turbotoggle.domain.entity.TargetingRule;

import java.util.List;
import java.util.Map;

public record FlagConfigPayloadDto(
        String flagKey,
        String flagType,
        boolean enabled,
        String defaultVariation,
        List<TargetingRule> rules,
        Map<String, String> individualTargets,
        long version
) {}