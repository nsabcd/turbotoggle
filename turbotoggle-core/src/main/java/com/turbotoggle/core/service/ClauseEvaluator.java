package com.turbotoggle.core.service;


import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.EvaluationContextDto;

import java.util.Collection;
import java.util.Locale;

import static com.turbotoggle.core.model.Operator.*;

public class ClauseEvaluator {
    public boolean evaluate(Clause clause, EvaluationContextDto context) {
        // Guard clause for null inputs
        if (clause == null || clause.getOperator() == null) {
            return false;
        }
        Object attributeValue = resolveAttributeValue(clause.getAttribute(), context);
        if (attributeValue == null) {
            return false;
        }

        String strAttrValue = String.valueOf(attributeValue);
        String clauseOp = clause.getOperator().toUpperCase();
        Collection<String> targetValues = clause.getValues();

        return switch (clauseOp) {
            case EQUALS -> targetValues.stream().anyMatch(val -> val.equalsIgnoreCase(strAttrValue));
            case NOT_EQUALS -> targetValues.stream().noneMatch(val -> val.equalsIgnoreCase(strAttrValue));
            case CONTAINS -> targetValues.stream().anyMatch(strAttrValue::contains);
            case NOT_CONTAINS -> targetValues.stream().noneMatch(strAttrValue::contains);
            case STARTS_WITH -> targetValues.stream().anyMatch(strAttrValue::startsWith);
            case ENDS_WITH -> targetValues.stream().anyMatch(strAttrValue::endsWith);
            case IN -> targetValues.contains(strAttrValue);
            case NOT_IN -> !targetValues.contains(strAttrValue);
            case GREATER_THAN -> compareNumbers(strAttrValue, targetValues) > 0;
            case LESS_THAN -> compareNumbers(strAttrValue, targetValues) < 0;
            case LESS_THAN_OR_EQUAL -> compareNumbers(strAttrValue, targetValues) <= 0;
            case GREATER_THAN_OR_EQUAL -> compareNumbers(strAttrValue, targetValues) >= 0;
            case IS_TRUE -> Boolean.parseBoolean(strAttrValue);
            case IS_FALSE -> !Boolean.parseBoolean(strAttrValue);
            default -> false;
        };
    }

    private Object resolveAttributeValue(String attribute, EvaluationContextDto context) {
        if ("key".equalsIgnoreCase(attribute) || "userId".equalsIgnoreCase(attribute)) {
            return context.key();
        }
        return context.attributes().get(attribute);
    }

    private int compareNumbers(String strAttrValue, Collection<String> targetValues) {
        try {
            double attrNum = Double.parseDouble(strAttrValue);
            double targetNum = Double.parseDouble(targetValues.iterator().next());
            return Double.compare(attrNum, targetNum);
        } catch (Exception e) {
            return 0;
        }
    }
}
