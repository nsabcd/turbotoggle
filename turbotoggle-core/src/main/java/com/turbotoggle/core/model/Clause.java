package com.turbotoggle.core.model;

import java.util.List;

public class Clause {
    private String attribute;   // e.g., "email", "country", "userId"[cite: 1]
    private String operator;    // e.g., "EQUALS", "CONTAINS", "IN", "GREATER_THAN"[cite: 1]
    private List<String> values; // e.g., ["US", "CA"][cite: 1]

    public Clause() {
    }

    public Clause(String attribute, String operator, List<String> values) {
        this.attribute = attribute;
        this.operator = operator;
        this.values = values;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}