package com.turbotoggle.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModelCoverageTest {
    @Test
    @DisplayName("Test Clause default constructors and setters")
    void testClauseSetters(){
        Clause clause = new Clause();
        clause.setAttribute("attr");
        clause.setOperator("EQUALS");
        clause.setValues(List.of("val"));

        assertEquals("attr", clause.getAttribute());
        assertEquals("EQUALS", clause.getOperator());
        assertEquals(List.of("val"), clause.getValues());
    }

    @Test
    @DisplayName("Test TargetingRule default constructors and setters")
    void testTargetingRuleSetters() {
        TargetingRule rule = new TargetingRule();
        rule.setRuleId("r1");
        rule.setVariation("v1");
        rule.setClauses(List.of());
        rule.setPercentageRollouts(List.of());

        assertEquals("r1", rule.getRuleId());
        assertEquals("v1", rule.getVariation());
        assertNotNull(rule.getClauses());
        assertNotNull(rule.getPercentageRollouts());
    }

    @Test
    @DisplayName("Test EvaluationRequestDto and BatchEvaluationResponseDto instantiations")
    void testDtoInstantiations() {
        EvaluationContextDto context = new EvaluationContextDto("u1", Map.of());
        EvaluationRequestDto request = new EvaluationRequestDto(context, List.of("flag1"));
        assertNotNull(request);

        BatchEvaluationResponseDto response = new BatchEvaluationResponseDto(Map.of());
        assertNotNull(response);
    }
}
