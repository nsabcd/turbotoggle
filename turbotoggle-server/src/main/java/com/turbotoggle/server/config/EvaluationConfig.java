package com.turbotoggle.server.config;

import com.turbotoggle.core.service.ClauseEvaluator;
import com.turbotoggle.core.service.EvaluatorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluationConfig {

    @Bean
    public ClauseEvaluator clauseEvaluator() {
        return new ClauseEvaluator();
    }

    @Bean
    public EvaluatorService evaluatorService(ClauseEvaluator clauseEvaluator) {
        return new EvaluatorService(clauseEvaluator);
    }
}