package com.turbotoggle.domain.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "environment_configs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_flag_env",
                columnNames = {"flag_id", "environment_id"}
        )
)
public class EnvironmentConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "flag_id", nullable = false)
    private FeatureFlag featureFlag;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "environment_id", nullable = false)
    private Environment environment;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "default_variation", nullable = false, columnDefinition = "TEXT")
    private String defaultVariation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules", nullable = false)
    private List<TargetingRule> rules = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "individual_targets", nullable = false)
    private Map<String, String> individualTargets = new HashMap<>();

    @Version
    @Column(nullable = false)
    private Long version = 1L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public EnvironmentConfig() {
    }

    public Long getId() {
        return Id;
    }

    public FeatureFlag getFeatureFlag() {
        return featureFlag;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDefaultVariation() {
        return defaultVariation;
    }

    public List<TargetingRule> getRules() {
        return rules;
    }

    public Map<String, String> getIndividualTargets() {
        return individualTargets;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setFeatureFlag(FeatureFlag featureFlag) {
        this.featureFlag = featureFlag;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setDefaultVariation(String defaultVariation) {
        this.defaultVariation = defaultVariation;
    }

    public void setRules(List<TargetingRule> rules) {
        this.rules = rules;
    }

    public void setIndividualTargets(Map<String, String> individualTargets) {
        this.individualTargets = individualTargets;
    }
}
