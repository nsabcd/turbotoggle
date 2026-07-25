package com.turbotoggle.domain.model;

import java.util.List;

public record TargetingRuleDto(
    int priority,
    String attribute,
    String operator,
    List<String> values,
    String variation
) {}
