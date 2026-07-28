package com.turbotoggle.server.repository;

import com.turbotoggle.server.domain.entity.Environment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    /**
     * Spring Data JPA automatically generates:
     * SELECT e FROM Environment e WHERE e.sdkKey = ?
     */
    Optional<Environment> findBySdkKey(String sdkKey);
    /**
     * Finds an environment by its unique environment key (e.g., "production", "staging").
     */
    Optional<Environment> findByEnvKey(String envKey);
}