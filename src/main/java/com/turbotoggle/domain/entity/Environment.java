package com.turbotoggle.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "environments")
public class Environment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "env_key", nullable = false, unique = true, length = 64)
    private String envKey;

    @Column(name = "sdk_key", nullable = false, unique = true, length = 255)
    private String sdkKey;

    @Column(nullable = false, length = 128)
    private String name;

    public Environment() {
    }

    public Environment(String envKey, String sdkKey, String name) {
        this.envKey = envKey;
        this.sdkKey = sdkKey;
        this.name = name;
    }

    public Long getId() {
        return Id;
    }

    public String getEnvKey() {
        return envKey;
    }

    public String getSdkKey() {
        return sdkKey;
    }

    public String getName() {
        return name;
    }

    public void setEnvKey(String envKey) {
        this.envKey = envKey;
    }

    public void setSdkKey(String sdkKey) {
        this.sdkKey = sdkKey;
    }

    public void setName(String name) {
        this.name = name;
    }
}
