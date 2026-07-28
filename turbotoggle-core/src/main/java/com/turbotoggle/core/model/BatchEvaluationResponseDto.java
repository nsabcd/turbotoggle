package com.turbotoggle.core.model;

import java.util.Map;

public record BatchEvaluationResponseDto(Map<String, EvaluationResultDto> flags) {
}
