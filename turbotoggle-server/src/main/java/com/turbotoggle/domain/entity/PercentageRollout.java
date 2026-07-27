package com.turbotoggle.domain.entity;

public class PercentageRollout {
    private String variation; // e.g., "treatment", "control", "true"
    private int percentage;

    public PercentageRollout() {
    }

    public PercentageRollout(String variation, int percentage) {
        this.variation = variation;
        this.percentage = percentage;
    }

    public String getVariation() {
        return variation;
    }

    public void setVariation(String variation) {
        this.variation = variation;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
