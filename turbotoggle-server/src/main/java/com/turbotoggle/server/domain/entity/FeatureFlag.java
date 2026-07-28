package com.turbotoggle.server.domain.entity;

import com.turbotoggle.server.domain.enums.FlagType;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "feature_flags")
public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, unique = true, length = 128)
    private String flagKey;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_type", nullable = false, length = 32)
    private FlagType flagType = FlagType.BOOLEAN;

    @Column(nullable = false)
    private boolean archived = false;

    // Optimistic locking field managed automatically by JPA
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate(){
        this.updatedAt = Instant.now();
    }

    public FeatureFlag() {
    }

    public FeatureFlag(String flagKey, String name, String description, FlagType flagType) {
        this.flagKey = flagKey;
        this.name = name;
        this.description = description;
        this.flagType = flagType;
    }

    public Long getId() {
        return id;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public FlagType getFlagType() {
        return flagType;
    }

    public boolean isArchived() {
        return archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setFlagKey(String flagKey) {
        this.flagKey = flagKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFlagType(FlagType flagType) {
        this.flagType = flagType;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getVersion() {
        return version;
    }
}
