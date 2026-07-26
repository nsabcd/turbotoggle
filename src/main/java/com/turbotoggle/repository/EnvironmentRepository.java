package com.turbotoggle.repository;

import com.turbotoggle.domain.entity.Environment;
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
}