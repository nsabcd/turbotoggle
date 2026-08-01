package com.turbotoggle.server.validation;

import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.PercentageRollout;
import com.turbotoggle.server.exception.InvalidRuleConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class RulePayloadValidatorTest {
    @Test
    void validateClause_shouldThrowException_whenOperatorOrValuesInvalid() {
        RulePayloadValidator validator = new RulePayloadValidator();

        // Test missing attribute / operator
        Clause invalidClause = new Clause(null, null, List.of("val"));

        assertThrows(InvalidRuleConfigurationException.class, () -> validator.validateClause(invalidClause));
    }

    @Test
    void validatePercentageRollout_shouldThrow_whenWeightsInvalid() {
        RulePayloadValidator validator = new RulePayloadValidator();


        PercentageRollout rollout1 = new PercentageRollout(
                "variation-a", 25
        );
        PercentageRollout rollout2 = new PercentageRollout(
                "variation-b", 25
        );
        PercentageRollout rollout3 = new PercentageRollout(
                "variation-c", 25
        );
        PercentageRollout rollout4 = new PercentageRollout(
                "variation-d", 24
        );
        List<PercentageRollout> invalidRollouts = List.of(rollout1, rollout2, rollout3, rollout4);

        assertThrows(InvalidRuleConfigurationException.class, () ->
                validator.validatePercentageRollout(invalidRollouts)
        );
    }
}
