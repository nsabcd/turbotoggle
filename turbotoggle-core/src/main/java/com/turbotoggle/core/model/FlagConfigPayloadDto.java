package com.turbotoggle.core.model;

import java.util.List;
import java.util.Map;

public record FlagConfigPayloadDto(String flagKey,
                                   String flagType,
                                   boolean enabled,
                                   String defaultVariation,
                                   List<TargetingRule> rules,
                                   Map<String, String> individualTargets,
                                   long version) {
}
