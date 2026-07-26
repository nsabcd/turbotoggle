package com.turbotoggle.service.evaluator;

import com.turbotoggle.domain.entity.Clause;
import com.turbotoggle.domain.entity.TargetingRule;
import com.turbotoggle.domain.model.EvaluationContextDto;
import com.turbotoggle.domain.model.EvaluationResultDto;
import com.turbotoggle.domain.model.FlagConfigPayloadDto;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

import java.util.List;

@Service
public class EvaluatorService {
    private static final Logger log = LoggerFactory.getLogger(EvaluatorService.class);
    private final ClauseEvaluator clauseEvaluator;

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
            return new EvaluationResultDto(flagKey, typedValue, variation, "FLAG_DISABLED", flag.version());
        }

        // 2. Individual Target Overrides (e.g., specific user target)
        if (flag.individualTargets() != null && context.key() != null) {
            String targetVariation = flag.individualTargets().get(context.key());
            if (targetVariation != null) {
                Object typedValue = resolveValueType(targetVariation, flagType);
                return new EvaluationResultDto(flagKey, typedValue, targetVariation, "INDIVIDUAL_TARGET", flag.version());}
        }

        // 3. Evaluate Sequential Targeting Rules
        if (flag.rules() != null && !flag.rules().isEmpty()) {
            for (TargetingRule rule : flag.rules()) {
                if (matchesRule(rule, context)) {
                    String resolvedVariation = resolveRolloutVariation(flagKey, context.key(), rule);
                    Object typedValue = resolveValueType(resolvedVariation, flagType);
                    return new EvaluationResultDto(flagKey, typedValue, resolvedVariation, "RULE_MATCH", flag.version());
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



    private Object resolveValueType(String rawValue, String flagType) {
        if (rawValue == null) return null;
        if ("BOOLEAN".equalsIgnoreCase(flagType)) {
            return Boolean.parseBoolean(rawValue);
        } else if ("NUMERIC".equalsIgnoreCase(flagType)) {
            try {
                return Double.parseDouble(rawValue);
            } catch (NumberFormatException e) {
                return rawValue;
            }
        }
        // Return raw string/JSON payload for STRING and JSON flag types
        return rawValue;
    }
}