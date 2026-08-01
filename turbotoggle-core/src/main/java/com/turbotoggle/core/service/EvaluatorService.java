package com.turbotoggle.core.service;


import com.turbotoggle.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;


public class EvaluatorService {
    private static final Logger log = LoggerFactory.getLogger(EvaluatorService.class);
    private final ClauseEvaluator clauseEvaluator;
    private static String FLAG_DISABLED_REASON = "FLAG_DISABLED";
    private static  String INDIVIDUAL_TARGET_REASON= "INDIVIDUAL_TARGET";
    private static String RULE_MATCH_REASON = "RULE_MATCH";

    public EvaluatorService(ClauseEvaluator clauseEvaluator) {
        this.clauseEvaluator = clauseEvaluator;
    }

    public EvaluationResultDto evaluate(FlagConfigPayloadDto flag, EvaluationContextDto context){
        String flagKey = flag.flagKey();
        String flagType = flag.flagType();

        // 1. Check if flag is disabled
        if (!Boolean.TRUE.equals(flag.enabled())) {
            String variation = flag.defaultVariation();
            Object typedValue = resolveValueType(variation, flagType);
            return new EvaluationResultDto(flagKey, typedValue, variation, FLAG_DISABLED_REASON, flag.version());
        }

        // 2. Individual Target Overrides (e.g., specific user target)
        if (flag.individualTargets() != null && context.key() != null) {
            String targetVariation = flag.individualTargets().get(context.key());
            if (targetVariation != null) {
                Object typedValue = resolveValueType(targetVariation, flagType);
                return new EvaluationResultDto(flagKey, typedValue, targetVariation, INDIVIDUAL_TARGET_REASON, flag.version());}
        }

        // 3. Evaluate Sequential Targeting Rules
        if (flag.rules() != null && !flag.rules().isEmpty()) {
            for (TargetingRule rule : flag.rules()) {
                if (matchesRule(rule, context)) {
                    String resolvedVariation = resolveRolloutVariation(flagKey, context.key(), rule);
                    Object typedValue = resolveValueType(resolvedVariation, flagType);
                    return new EvaluationResultDto(flagKey, typedValue, resolvedVariation, RULE_MATCH_REASON, flag.version());
                }
            }
        }

        // 4. Default Fallback
        String fallbackVariation = flag.defaultVariation();
        Object typedValue = resolveValueType(fallbackVariation, flagType);
        return new EvaluationResultDto(flagKey, typedValue, fallbackVariation, "DEFAULT_FALLBACK", flag.version());
    }

    private boolean matchesRule(TargetingRule rule, EvaluationContextDto context) {
        List<Clause> clauses = rule.getClauses();
        if (clauses == null || clauses.isEmpty()) {
            return false;
        }
        // ALL clauses in a rule must evaluate to true (AND logic)
        return clauses.stream().allMatch(clause -> clauseEvaluator.evaluate(clause, context));
    }

    private String resolveRolloutVariation(String flagKey, String contextKey, TargetingRule rule) {
        // Percentage Rollout (e.g., 20% treatment, 80% control)
        if (rule.getPercentageRollouts() != null && !rule.getPercentageRollouts().isEmpty()) {
            int bucket = HashingUtil.getBucket(flagKey, contextKey != null ? contextKey : "anonymous");
            int cumulativePercentage = 0;

            for (var rollout : rule.getPercentageRollouts()) {
                cumulativePercentage += rollout.getPercentage();
                if (bucket < cumulativePercentage) {
                    return rollout.getVariation();
                }
            }
        }
        return rule.getVariation();
    }



    private Object resolveValueType(String rawValue, String valueType) {
        if (rawValue == null) return null;
        if (rawValue == null || valueType == null) {
            return rawValue;
        }
        return switch (valueType.toUpperCase()) {
            case "BOOLEAN" -> Boolean.parseBoolean(rawValue);
            case "INTEGER", "INT" -> Integer.parseInt(rawValue);
            case "DOUBLE", "FLOAT" -> Double.parseDouble(rawValue);
            default -> rawValue; // Default fallback to String (JSON, STRING, etc.)
        };

    }
}