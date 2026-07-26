package com.turbotoggle.domain.entity;

import java.util.List;
import java.util.Objects;

public class Clause {
    private String attribute;  // e.g., "email", "country", "plan", "userId"
    private String operator;   // e.g., "EQUALS", "CONTAINS", "IN", "GREATER_THAN"
    private List<String> values; // e.g., ["US", "CA"] or ["@company.com"]

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clause clause = (Clause) o;
        return Objects.equals(attribute, clause.attribute) &&
                Objects.equals(operator, clause.operator) &&
                Objects.equals(values, clause.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, operator, values);
    }
}
