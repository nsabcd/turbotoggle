package com.turbotoggle.server.domain.model;

import com.turbotoggle.core.model.TargetingRule;

import java.util.List;
import java.util.Map;

public record UpdateConfigRulesRequestDto (
        Boolean enabled,
        String defaultVariation,
        List<TargetingRule> rules,
        Map<String, String> individualTargets
) {
}
