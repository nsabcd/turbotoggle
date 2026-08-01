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
import java.util.Collections;
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
        Clause notEqualsClause = new Clause("role", NOT_EQUALS, List.of("GUEST"));
        EvaluationContextDto context = new EvaluationContextDto(
                "user-2",
                Map.of("role","ADMIN")
        );
        assertTrue(clauseEvaluator.evaluate(inClause, context));
        assertTrue(clauseEvaluator.evaluate(notInClause, context));
        assertTrue(clauseEvaluator.evaluate(notEqualsClause, context));
    }

    @Test
    @DisplayName("Should evaluate string matching operators (CONTAINS, STARTS_WITH, ENDS_WITH)")
    void shouldEvaluateStringMatchingOperators() {
        EvaluationContextDto context = new EvaluationContextDto(
                "user-string",
                Map.of("email", "dev@turbotoggle.io")
        );

        Clause contains = new Clause("email", CONTAINS, List.of("turbotoggle"));
        Clause startsWith = new Clause("email", STARTS_WITH, List.of("dev@"));
        Clause endsWith = new Clause("email", ENDS_WITH, List.of(".io"));

        assertTrue(clauseEvaluator.evaluate(contains, context));
        assertTrue(clauseEvaluator.evaluate(startsWith, context));
        assertTrue(clauseEvaluator.evaluate(endsWith, context));
    }

    @ParameterizedTest
    @CsvSource({
            "2.5, GREATER_THAN, 2.0, true",
            "1.5, GREATER_THAN, 2.0, false",
            "2.0, GREATER_THAN_OR_EQUAL, 2.0, true",
            "1.0, LESS_THAN, 2.0, true",
            "3.0, LESS_THAN, 2.0, false",
            "2.0, LESS_THAN_OR_EQUAL, 2.0, true"
    })
    @DisplayName("Should evaluate numeric comparisons correctly")
    void shouldEvaluateNumeriCOperations(String attrVal, String operator, String targetVal, boolean expectedResult){
        Clause clause = new Clause("version", operator, List.of(targetVal));
        EvaluationContextDto context = new EvaluationContextDto(
                "user-3",
                Map.of("version", attrVal)
        );
        boolean evaluation = clauseEvaluator.evaluate(clause, context);
        assertEquals(clauseEvaluator.evaluate(clause, context), expectedResult);
    }

    @Test
    @DisplayName("Should handle NumberFormatException and empty target values in compareNumbers gracefully")
    void shouldHandleNumericEdgeCases() {
        // Non-parsable number context value
        Clause clauseInvalidNumber = new Clause("version", GREATER_THAN, List.of("2.0"));
        EvaluationContextDto contextInvalid = new EvaluationContextDto("u1", Map.of("version", "not-a-number"));
        assertFalse(clauseEvaluator.evaluate(clauseInvalidNumber, contextInvalid));

        // Empty target values list
        Clause clauseEmptyValues = new Clause("version", GREATER_THAN, Collections.emptyList());
        EvaluationContextDto contextValid = new EvaluationContextDto("u1", Map.of("version", "2.0"));
        assertFalse(clauseEvaluator.evaluate(clauseEmptyValues, contextValid));
    }

    @Test
    @DisplayName("Should return false when attribute is missing from context")
    void shouldReturnFalseWhenAttributeIsMissing(){
        Clause missingAttrClause = new Clause("non_existent_attribute", "EQUALS", List.of("value"));
        EvaluationContextDto context = new EvaluationContextDto("user-4", Map.of("country", "US"));

        assertFalse(clauseEvaluator.evaluate(missingAttrClause, context));

        // Null clause check
        assertFalse(clauseEvaluator.evaluate(null, context));
    }
}
