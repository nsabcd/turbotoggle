package com.turbotoggle.server.domain.model;

import com.turbotoggle.server.domain.enums.FlagType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateFlagRequestDto(
        @NotBlank(message = "Flag key is required") String flagKey,
        @NotBlank(message = "Flag name is requred") String name,
        String description,
        @NotNull(message = "Flag type is required")
        FlagType flagType
        ) {
}
