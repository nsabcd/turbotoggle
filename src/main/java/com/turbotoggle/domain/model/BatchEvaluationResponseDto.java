package com.turbotoggle.domain.model;

import java.util.Map;

public record BatchEvaluationResponseDto(Map<String, EvaluationResultDto> flags) {
}
