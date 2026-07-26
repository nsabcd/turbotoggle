package com.turbotoggle.domain.entity;

import java.util.List;

public class TargetingRule {

    private String ruleId;
    private String variation;                          // Single variation if not using percentage rollouts
    private List<Clause> clauses;                      // List of conditions (AND logic)
    private List<PercentageRollout> percentageRollouts; // Optional weighted variations (e.g., 50/50 split)

    public TargetingRule() {
    }

    public TargetingRule(String ruleId, String variation, List<Clause> clauses, List<PercentageRollout> percentageRollouts) {
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