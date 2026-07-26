package com.turbotoggle.repository;

import com.turbotoggle.domain.entity.EnvironmentConfig;
import com.turbotoggle.domain.model.FlagConfigPayloadDto;
import org.springframework.data.repository.query.Param;
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
    @Query("""
        SELECT new com.turbotoggle.domain.model.FlagConfigPayloadDto(
            f.flagKey,
            CAST(f.flagType AS string),
            ec.enabled,
            ec.defaultVariation,
            ec.rules,
            ec.individualTargets,
            ec.version
        )
        FROM EnvironmentConfig ec
        JOIN ec.featureFlag f
        JOIN ec.environment e
        WHERE e.sdkKey = :sdkKey
          AND f.archived = false
        """)
    List<FlagConfigPayloadDto> findAllFlagPayloadDtosBySdkKey(@Param("sdkKey") String sdkKey);

    /**
     * Fetch managed entities for single update operation in control plan
     */
    @Query("""
        SELECT ec FROM EnvironmentConfig ec
        JOIN FETCH ec.featureFlag f
        JOIN FETCH ec.environment e
        WHERE f.flagKey = :flagKey 
          AND e.sdkKey = :sdkKey
          AND f.archived = false
        """)
    Optional<EnvironmentConfig> findByFlagKeyAndSdkKey(
            @Param("flagKey") String flagKey,
            @Param("sdkKey") String sdkKey
    );

    @Query("""
    SELECT new com.turbotoggle.domain.model.FlagConfigPayloadDto(
        f.flagKey,
        CAST(f.flagType AS string),
        ec.enabled,
        ec.defaultVariation,
        ec.rules,
        ec.individualTargets,
        ec.version
    )
    FROM EnvironmentConfig ec
    JOIN ec.featureFlag f
    JOIN ec.environment e
    WHERE f.flagKey = :flagKey 
      AND e.sdkKey = :sdkKey
      AND f.archived = false
    """)
    Optional<FlagConfigPayloadDto> findPayloadDtoByFlagKeyAndSdkKey(
            @Param("flagKey") String flagKey,
            @Param("sdkKey") String sdkKey
    );

}
