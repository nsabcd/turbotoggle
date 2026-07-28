package com.turbotoggle.core.model;


import java.util.List;

public record EvaluationRequestDto (
        EvaluationContextDto context,

        // Optional: If provided, evaluates ONLY these specific flag keys.
        // If null/empty, evaluates ALL flags in the environment.
        List<String> flagKeys
) {
}
