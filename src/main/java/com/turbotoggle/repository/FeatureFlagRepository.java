package com.turbotoggle.repository;

import com.turbotoggle.domain.entity.Environment;
import com.turbotoggle.domain.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag, Long> {
    Optional<FeatureFlag> findByFlagKey(String flagKey);
}
