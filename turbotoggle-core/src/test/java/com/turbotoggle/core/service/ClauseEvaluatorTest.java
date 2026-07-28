package com.turbotoggle.core.service;

import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.EvaluationContextDto;
import com.turbotoggle.core.model.EvaluationResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.turbotoggle.core.model.Operator.*;
import static org.junit.jupiter.api.Assertions.*;

public class ClauseEvaluatorTest {
    private ClauseEvaluator clauseEvaluator;

    @BeforeEach
    void setUp(){
        clauseEvaluator = new ClauseEvaluator();
    }

    @Test
    @DisplayName("Should evaluate EQUALS operator correctly for matching context attribute")
    void shouldEvaluateEqualsOperator(){
        Clause clause = new Clause("country", EQUALS, List.of("US"));
        EvaluationContextDto context = new EvaluationContextDto(
                "user-1",
                Map.of("country","US")
        );
        assertTrue(clauseEvaluator.evaluate(clause, context));
    }

    @Test
    @DisplayName("Should resolve 'key' attribute directly from EvaluationContextDto key field")
    void shouldResolveKeyAttributeFromContext(){
        Clause clause = new Clause("key", EQUALS, List.of("admin-user"));
        EvaluationContextDto context = new EvaluationContextDto(
                "admin-user",
                Map.of()
        );
        assertTrue(clauseEvaluator.evaluate(clause, context));
    }

    @Test
    @DisplayName("Should evaluate IN and NOT_IN operators correctly")
    void shouldEvaluateInAndNotInOperators(){
        Clause inClause = new Clause("role", IN, List.of("ADMIN", "BETA_USERS"));
        Clause notInClause = new Clause("role", NOT_IN, List.of("GUEST", "APPLICATION"));
        EvaluationContextDto context = new EvaluationContextDto(
                "user-2",
                Map.of("role","ADMIN")
        );
        assertTrue(clauseEvaluator.evaluate(inClause, context));
        assertTrue(clauseEvaluator.evaluate(notInClause, context));
    }

    @ParameterizedTest
    @CsvSource({
            "2.5,GREATER_THAN,2.0, true",
            "1.5,GREATER_THAN, 2.0, false",
            "1.0,LESS_THAN, 2.0, true",
            "3.0,LESS_THAN, 2.0, false"
    })
    @DisplayName("Should evaluate numeric comparisons correctly")
    void shouldEvaluateNumeriOperations(String attrVal, String operator, String targetVal, boolean expectedResult){
        Clause clause = new Clause("version", operator, List.of(targetVal));
        EvaluationContextDto context = new EvaluationContextDto(
                "user-3",
                Map.of("version", attrVal)
        );
        boolean evaluation = clauseEvaluator.evaluate(clause, context);
        assertEquals(clauseEvaluator.evaluate(clause, context), expectedResult);
    }

    @Test
    @DisplayName("Should return false when attribute is missing from context")
    void shouldReturnFalseWhenAttributeIsMissing(){
        Clause clause = new Clause("non_existent_attribute", "EQUALS", List.of("value"));
        EvaluationContextDto context = new EvaluationContextDto("user-4", Map.of("country", "US"));

        assertFalse(clauseEvaluator.evaluate(clause, context));
    }
}
