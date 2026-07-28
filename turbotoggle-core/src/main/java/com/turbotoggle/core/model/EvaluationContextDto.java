package com.turbotoggle.core.model;

import java.util.Collections;
import java.util.Map;

public record EvaluationContextDto(
        String key,
        Map<String, Object> attributes
) {
    public EvaluationContextDto {
        if(attributes==null){
            attributes = Collections.emptyMap();
        }
    }
}
