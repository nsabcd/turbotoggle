package com.turbotoggle.server.domain.model;

import jakarta.validation.constraints.NotBlank;

public record CreateEnvironmentRequestDto(
        @NotBlank(message = "Env key is required") String envKey,
        @NotBlank(message = "Name is required") String name
) {
}
