package com.turbotoggle.domain.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EvaluationRequestDto (
        @NotNull(message = "Evaluation context is required")
        @Valid
        EvaluationContextDto context,

        // Optional: If provided, evaluates ONLY these specific flag keys.
        // If null/empty, evaluates ALL flags in the environment.
        List<String> flagKeys
) {
}
