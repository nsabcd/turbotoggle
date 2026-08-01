package com.turbotoggle.core.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TargetingRule {

    private String ruleId;
    private String variation;                          // Default variation if not using percentage rollouts[cite: 1]
    private List<Clause> clauses;                      // List of conditions (AND logic)[cite: 1]
    private List<PercentageRollout> percentageRollouts; // Optional weighted variations[cite: 1]

    public TargetingRule() {
    }

    @JsonCreator
    public TargetingRule(@JsonProperty("ruleId") String ruleId,
                         @JsonProperty("variation") String variation,
                         @JsonProperty("clauses") List<Clause> clauses,
                         @JsonProperty("percentageRollouts") List<PercentageRollout> percentageRollouts) {
        this.ruleId = ruleId;
        this.variation = variation;
        this.clauses = clauses;
        this.percentageRollouts = percentageRollouts;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getVariation() {
        return variation;
    }

    public void setVariation(String variation) {
        this.variation = variation;
    }

    public List<Clause> getClauses() {
        return clauses;
    }

    public void setClauses(List<Clause> clauses) {
        this.clauses = clauses;
    }

    public List<PercentageRollout> getPercentageRollouts() {
        return percentageRollouts;
    }

    public void setPercentageRollouts(List<PercentageRollout> percentageRollouts) {
        this.percentageRollouts = percentageRollouts;
    }
}