package com.turbotoggle.server.domain.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "flag_audit_logs")
public class FlagAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 128)
    private String flagKey;

    @Column(name = "action_type", nullable = false, length = 64)
    private String actionType; // e.g., "CREATED", "MUTATED", "ARCHIVED"

    @Column(name = "performed_by", nullable = false)
    private String performedBy; // Username or API Token ID

    @Column(name = "previous_state_json", columnDefinition = "TEXT")
    private String previousStateJson;

    @Column(name = "new_state_json", columnDefinition = "TEXT")
    private String newStateJson;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @PrePersist
    public void onCreate() {
        this.timestamp = Instant.now();
    }

    public FlagAuditLog() {}

    public FlagAuditLog(String flagKey, String actionType, String performedBy, String previousStateJson, String newStateJson) {
        this.flagKey = flagKey;
        this.actionType = actionType;
        this.performedBy = performedBy;
        this.previousStateJson = previousStateJson;
        this.newStateJson = newStateJson;
    }

    // Getters
    public Long getId() { return id; }
    public String getFlagKey() { return flagKey; }
    public String getActionType() { return actionType; }
    public String getPerformedBy() { return performedBy; }
    public String getPreviousStateJson() { return previousStateJson; }
    public String getNewStateJson() { return newStateJson; }
    public Instant getTimestamp() { return timestamp; }
}

