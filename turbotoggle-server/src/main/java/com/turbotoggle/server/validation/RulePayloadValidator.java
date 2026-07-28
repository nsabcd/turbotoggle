package com.turbotoggle.server.validation;

import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.core.model.PercentageRollout;
import com.turbotoggle.core.model.TargetingRule;
import com.turbotoggle.server.exception.InvalidRuleConfigurationException;
import org.springframework.stereotype.Component;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import java.util.List;

import static com.turbotoggle.core.model.Operator.*;

@Component
public class RulePayloadValidator {
    public void validate(FlagConfigPayloadDto payload){
        if (payload == null) {
            throw new InvalidRuleConfigurationException("Flag payload cannot be null.");
        }

        if (payload.flagKey() == null || payload.flagKey().isBlank()) {
            throw new InvalidRuleConfigurationException("Flag key cannot be null or empty.");
        }

        // 1. Validate Default Variation
        if (payload.defaultVariation() == null || payload.defaultVariation().isBlank()) {
            throw new InvalidRuleConfigurationException("Default variation cannot be null or empty.");
        }

        // 2. Validate Targeting Rules
        if (payload.rules() != null) {
            for (TargetingRule rule : payload.rules()) {
                validateTargetingRule(rule);
            }
        }
    }

    private void validateTargetingRule(TargetingRule rule) {
        if (rule == null) {
            throw new InvalidRuleConfigurationException("Targeting rule cannot be null.");
        }

        // A rule must supply either a single variation OR a list of percentage rollouts
        boolean hasVariation = rule.getVariation() != null && !rule.getVariation().isBlank();
        boolean hasRollouts = rule.getPercentageRollouts() != null && !rule.getPercentageRollouts().isEmpty();

        if (!hasVariation && !hasRollouts) {
            throw new InvalidRuleConfigurationException(
                    "Targeting rule [" + rule.getRuleId() + "] must specify either a variation or percentage rollouts.");
        }

        if (rule.getClauses() == null || rule.getClauses().isEmpty()) {
            throw new InvalidRuleConfigurationException(
                    "Targeting rule [" + rule.getRuleId() + "] must contain at least one clause.");
        }

        // Validate clauses inside this targeting rule
        for (Clause clause : rule.getClauses()) {
            validateClause(clause);
        }

        // Validate percentage rollouts if present
        if (hasRollouts) {
            validatePercentageRollout(rule.getPercentageRollouts());
        }
    }
    private void validatePercentageRollout(List<PercentageRollout> rollouts) {
        double totalPercentage = 0.0;

        for (PercentageRollout rollout : rollouts) {
            if (rollout.getPercentage() < 0.0 || rollout.getPercentage() > 100.0) {
                throw new InvalidRuleConfigurationException(
                        "Rollout percentage must be between 0 and 100. Found: " + rollout.getPercentage());
            }
            totalPercentage += rollout.getPercentage();
        }

        // Check that percentages sum to 100%
        if (Math.abs(totalPercentage - 100.0) > 0.001) {
            throw new InvalidRuleConfigurationException(
                    "Target variant rollout percentages must sum to exactly 100%. Total was: " + totalPercentage);
        }
    }

    private void validateClause(Clause clause) {
        if (clause == null) {
            throw new InvalidRuleConfigurationException("Clause cannot be null.");
        }

        if (clause.getAttribute() == null || clause.getAttribute().isBlank()) {
            throw new InvalidRuleConfigurationException("Clause attribute cannot be empty.");
        }

        if (clause.getOperator() == null) {
            throw new InvalidRuleConfigurationException("Clause operator cannot be null.");
        }

        if (clause.getValues() == null || clause.getValues().isEmpty()) {
            throw new InvalidRuleConfigurationException("Clause must contain at least one comparison value.");
        }

        switch (clause.getOperator()) {
            case GREATER_THAN, LESS_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL -> {
                for (String val : clause.getValues()) {
                    try {
                        Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        throw new InvalidRuleConfigurationException(
                                "Operator " + clause.getOperator() + " requires numeric values. Found: " + val);
                    }
                }
            }
            case REGEX -> {
                for (String val : clause.getValues()) {
                    try {
                        Pattern.compile(val);
                    } catch (PatternSyntaxException e) {
                        throw new InvalidRuleConfigurationException("Invalid REGEX pattern: " + val);
                    }
                }
            }
            case IN, NOT_IN, EQUALS, NOT_EQUALS -> {
                // String comparison validation
            }
        }
    }
}
