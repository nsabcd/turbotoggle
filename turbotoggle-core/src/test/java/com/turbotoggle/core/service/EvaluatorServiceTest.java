package com.turbotoggle.core.service;

import com.turbotoggle.core.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

public class EvaluatorServiceTest {
    private static String FLAG_DISABLED_REASON = "FLAG_DISABLED";
    private static  String INDIVIDUAL_TARGET_REASON= "INDIVIDUAL_TARGET";
    private static String RULE_MATCH_REASON = "RULE_MATCH";

    private EvaluatorService evaluatorService;
    @BeforeEach
    void setUp(){
        ClauseEvaluator clauseEvaluator = new ClauseEvaluator();
        evaluatorService = new EvaluatorService(clauseEvaluator);
    }

    @Test
    @DisplayName("Should return default variation when flag is disabled")
    void shouldReturnDefaultVariationWhenFlagIsDisabled(){
        FlagConfigPayloadDto flag = new FlagConfigPayloadDto(
                "checkout-v2",
                "BOOLEAN",
                false,
                "false",
                List.of(),
                Map.of(),
                1L
        );
        EvaluationContextDto context = new EvaluationContextDto(
                "user-123",
                Map.of("country", "US")
        );
        EvaluationResultDto result = evaluatorService.evaluate(flag, context);

        assertFalse((Boolean)result.value());
        assertEquals(FLAG_DISABLED_REASON, result.reason());
        assertEquals("false", result.variation());
    }

    @Test
    @DisplayName("Should evaluate individual target override over rules and default variation")
    void shouldEvaluateIndividualTarget() {
        FlagConfigPayloadDto flag = new FlagConfigPayloadDto(
                "new-checkout",
                "BOOLEAN",
                true,
                "false",
                List.of(),
                Map.of("user-vip-100", "true"),
                1L
        );
        EvaluationContextDto context = new EvaluationContextDto(
                "user-vip-100",
                Map.of()
        );

        EvaluationResultDto result = evaluatorService.evaluate(flag, context);
        assertEquals(INDIVIDUAL_TARGET_REASON, result.reason());
        assertEquals("true", result.variation());
        assertEquals(true, result.value());
    }

    @Test
    @DisplayName("Should match rule based on clause context and resolve variation")
    void shouldMatchRuleAndResolveVariation() {
        Clause clause = new Clause("tier", "EQUALS", List.of("PREMIUM"));
        TargetingRule rule = new TargetingRule("rule-1", "treatment-a", List.of(clause), List.of());

        FlagConfigPayloadDto flag = new FlagConfigPayloadDto(
                "premium-feature",
                "STRING",
                true,
                "control",
                List.of(rule),
                Map.of(),
                1L
        );
        EvaluationContextDto context = new EvaluationContextDto("user-200", Map.of("tier", "PREMIUM"));

        EvaluationResultDto result = evaluatorService.evaluate(flag, context);

        assertEquals(RULE_MATCH_REASON, result.reason());
        assertEquals("treatment-a", result.variation());
        assertEquals("treatment-a", result.value());
    }

    @Test
    @DisplayName("Should assign deterministic percentage rollout variation using MurmurHash3 bucketing")
    void shouldEvaluatePercentageRolloutDeterministically() {
        List<PercentageRollout> rollouts = List.of(
                new PercentageRollout("variant-a", 50),
                new PercentageRollout("variant-b", 50)
        );
        Clause catchAllClause = new Clause("key", "NOT_IN", List.of("empty"));
        TargetingRule rule = new TargetingRule("rule-rollout", null, List.of(catchAllClause), rollouts);

        FlagConfigPayloadDto flag = new FlagConfigPayloadDto(
                "rollout-flag",
                "STRING",
                true,
                "control",
                List.of(rule),
                Map.of(),
                1L
        );

        EvaluationContextDto contextUser1 = new EvaluationContextDto("user-alpha", Map.of());
        EvaluationContextDto contextUser2 = new EvaluationContextDto("user-alpha", Map.of()); // Same key

        EvaluationResultDto result1 = evaluatorService.evaluate(flag, contextUser1);
        EvaluationResultDto result2 = evaluatorService.evaluate(flag, contextUser2);

        // Determinism Check: Same context key must produce identical variation
        assertEquals(result1.variation(), result2.variation());
        assertEquals(RULE_MATCH_REASON, result1.reason());
    }

    @Test
    @DisplayName("Should return default fallback variation when flag is disabled")
    void shouldReturnDefaultFallbackWhenDisabled() {
        FlagConfigPayloadDto flag = new FlagConfigPayloadDto(
                "disabled-flag",
                "BOOLEAN",
                false,
                "false",
                List.of(),
                Map.of(),
                2L
        );
        EvaluationContextDto context = new EvaluationContextDto("user-300", Map.of());

        EvaluationResultDto result = evaluatorService.evaluate(flag, context);

        assertEquals(FLAG_DISABLED_REASON, result.reason());
        assertEquals("false", result.variation());
        assertEquals(false, result.value());
    }

}
