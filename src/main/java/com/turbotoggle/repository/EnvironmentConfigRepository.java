package com.turbotoggle.repository;

import com.turbotoggle.domain.entity.EnvironmentConfig;
import com.turbotoggle.repository.projection.FlagPayloadProjection;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnvironmentConfigRepository extends JpaRepository<EnvironmentConfig, Long> {
    /**
     * Fast Bulk retrieval of all active flags for a given environment by sdkKey
     * Bypass JPA
     * Use projection with Redis caching and Server Side Event (SSE) streaming
     */
    @Query(value = """
            SELECT
                f.flag_key AS flagKey,
                f.flag_type AS flagType,
                ec.enabled AS enabled,
                ec.default_variation AS defaultVariation,
                CAST(ec.rules AS TEXT) AS rulesJson,
                CAST(ec.individual_targets AS TEXT) AS individualTargetJson
                ec.version as version,
            FROM environment_configs ec
            INNER JOIN feature_flags f on ec.flag_id = f.id
            INNER JOIN environment e on ec.environment_id = e.id
            WHERE e.sdk_key = :sdkKey
            AND f.archived = FLASE
            """,nativeQuery = true)
    List<FlagPayloadProjection> findAllActiveFlagPayloadsBySdkKey(@Param("sdkKey") String sdkKey);

    /**
     * Fetch managed entities for single update operation in control plan
     */
    @Query(
            """
            SELECT ec FROM EnvironmentConfig ec
            JOIN FETCH ec.featureFlag f
            JOIN FETCH ec.environment e
            WHERE f.flagKey = :flagKey
            AND e.sdkKey = :sdkKey
            AND f.archived = false
            """
    )
    Optional<EnvironmentConfig> findByFlagAndSdkKey(
        @Param("flagKey") String flagKey,
        @Param("sdkKey") String sdkKey
    );

    /**
     * Fetch environment key associated with sdkKey for pub/sub channel resolution
     */
    @Query("SELECT e.envKey FROM environment as e WHERE e.sdkKey = :sdkKey")
    Optional<String> findEnvKeyBySdkKey(@Param("sdkKey") String sdkKey);
}
