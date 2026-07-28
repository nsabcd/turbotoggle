package com.turbotoggle.core.model;

public record EvaluationResultDto(
        String flagKey,
        Object value,            // The resolved variation value (e.g., "treatment", true, 50)
        String variation,        // Variation identifier/name
        String reason,           // Evaluation reason (e.g., "INDIVIDUAL_TARGET", "RULE_MATCH", "DEFAULT_FALLBACK", "FLAG_DISABLED")
        Long version
) {
}
