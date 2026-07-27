package com.turbotoggle.domain.model;

import com.turbotoggle.domain.enums.FlagType;
import jakarta.validation.constraints.NotBlank;

public record CreateFlagRequestDto(
        @NotBlank(message = "Flag key is required") String flagKey,
        @NotBlank(message = "Flag name is requred") String name,
        String description,
        @NotBlank(message = "Flag type is required")FlagType flagType
        ) {
}
