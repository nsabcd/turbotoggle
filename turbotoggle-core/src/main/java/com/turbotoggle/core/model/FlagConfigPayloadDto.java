package com.turbotoggle.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record FlagConfigPayloadDto(
        @JsonProperty("flagKey") String flagKey,
        @JsonProperty("flagType") String flagType,
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("defaultVariation") String defaultVariation,
        @JsonProperty("rules") List<TargetingRule> rules,
        @JsonProperty("individualTargets") Map<String, String> individualTargets,
        @JsonProperty("version") Long version) {
}
