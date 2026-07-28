package com.turbotoggle.server.service;

import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.server.domain.entity.Environment;
import com.turbotoggle.server.domain.entity.EnvironmentConfig;
import com.turbotoggle.server.domain.entity.FeatureFlag;
import com.turbotoggle.server.domain.enums.FlagType;
import com.turbotoggle.server.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.server.domain.model.CreateFlagRequestDto;
import com.turbotoggle.server.domain.model.UpdateConfigRulesRequestDto;
import com.turbotoggle.server.infrastructure.RedisMutationPublisher;
import com.turbotoggle.server.repository.EnvironmentConfigRepository;
import com.turbotoggle.server.repository.FeatureFlagRepository;

import com.turbotoggle.server.validation.RulePayloadValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class FlagManagementService {
    private static final Logger log = LoggerFactory.getLogger(FlagManagementService.class);
    private final FeatureFlagRepository featureFlagRepository;
    private final EnvironmentConfigRepository environmentConfigRepository;
    private final EnvironmentManagementService environmentManagementService;
    private final RedisMutationPublisher mutationPublisher;
    private final RulePayloadValidator rulePayloadValidator;
    private final AuditLogService auditLogService;
    private static final String FLAG_CREATED = "FLAG_CREATED";
    private static final String  RULES_MUTATED = "RULES_MUTATED";
    private static final String FLAG_ARCHIVED="FLAG_ARCHIVED";

    public FlagManagementService(
            FeatureFlagRepository featureFlagRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            EnvironmentManagementService environmentManagementService,
            RedisMutationPublisher mutationPublisher,
            RulePayloadValidator rulePayloadValidator,
            AuditLogService auditLogService) {
        this.featureFlagRepository = featureFlagRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.environmentManagementService = environmentManagementService;
        this.mutationPublisher=mutationPublisher;
        this.rulePayloadValidator=rulePayloadValidator;
        this.auditLogService=auditLogService;
    }

    public FeatureFlag createFlag(CreateFlagRequestDto request, String performedBy){

        if(featureFlagRepository.findByFlagKey(request.flagKey()).isPresent()){
            throw new IllegalArgumentException("Flag key already exists: " + request.flagKey());
        }

        FeatureFlag flag = new FeatureFlag();
        flag.setFlagKey(request.flagKey());
        flag.setName(request.name());
        flag.setDescription(request.description());
        flag.setFlagType(request.flagType());
        FeatureFlag savedFlag = featureFlagRepository.save(flag);
        log.info("Created feature flag [{}]", request.flagKey());

        // Audit log creation
        auditLogService.recordMutation(request.flagKey(), FLAG_CREATED, performedBy, null, savedFlag);

        // Bootstrap EnvironmentConfig for every existing environment
        List<Environment> environments = environmentManagementService.getAllEnvironments();
        for (Environment env : environments) {
            EnvironmentConfig config = new EnvironmentConfig();
            config.setFeatureFlag(savedFlag);
            config.setEnvironment(env);
            config.setEnabled(false);
            config.setDefaultVariation(request.flagType() == FlagType.BOOLEAN ? "false" : "default");
            environmentConfigRepository.save(config);
        }

        log.info("Created feature flag [{}] across {} environments", request.flagKey(), environments.size());
        return savedFlag;
    }

    public EnvironmentConfig updateConfigRules(String envKey, String flagKey, UpdateConfigRulesRequestDto request, String performedBy){
        Environment env = environmentManagementService.getByEnvKey(envKey);

        EnvironmentConfig config = environmentConfigRepository.findByEnvKeyAndFlagKey(envKey, flagKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Config not found for flag [%s] in env [%s]", flagKey, envKey)));

        // 1. Prepare proposed DTO for validation & auditing
        boolean nextEnabled = request.enabled() != null ? request.enabled() : config.isEnabled();
        String nextDefaultVar = request.defaultVariation() != null ? request.defaultVariation() : config.getDefaultVariation();
        var nextRules = request.rules() != null ? request.rules() : config.getRules();
        var nextTargets = request.individualTargets() != null ? request.individualTargets() : config.getIndividualTargets();
        long nextVersion = config.getVersion() + 1;

        FlagConfigPayloadDto proposedPayload = new FlagConfigPayloadDto(
                flagKey,
                config.getFeatureFlag().getFlagType().toString(),
                nextEnabled,
                nextDefaultVar,
                nextRules != null ? new ArrayList<>(nextRules) : List.of(),
                nextTargets,
                nextVersion
        );

        // 2. Validate payload before writing to DB
        rulePayloadValidator.validate(proposedPayload);

        // Snapshot previous state for Audit Logging
        FlagConfigPayloadDto previousPayload = new FlagConfigPayloadDto(
                flagKey,
                config.getFeatureFlag().getFlagType().toString(),
                config.isEnabled(),
                config.getDefaultVariation(),
                config.getRules() != null ? new ArrayList<>(config.getRules()) : List.of(),
                config.getIndividualTargets(),
                config.getVersion()
        );

        // 3. Update fields
        config.setEnabled(nextEnabled);
        config.setDefaultVariation(nextDefaultVar);
        config.setRules(nextRules);
        config.setIndividualTargets(nextTargets);
        config.setVersion(nextVersion);

        EnvironmentConfig savedConfig = environmentConfigRepository.save(config);
        log.info("Updated rules for flag [{}] in env [{}], new version [{}]", flagKey, envKey, savedConfig.getVersion());

        // 4. Record Audit Log
        auditLogService.recordMutation(flagKey, RULES_MUTATED, performedBy, previousPayload, proposedPayload);

        // 5. Publish mutation event
        FlagConfigUpdatedEvent event = new FlagConfigUpdatedEvent(
                env.getSdkKey(),
                flagKey,
                savedConfig.getVersion(),
                proposedPayload);

        mutationPublisher.publishMutation(event);

        return savedConfig;
    }
    @Transactional(readOnly = true)
    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAll();
    }

    public FeatureFlag archiveFlag(String flagKey, String performedBy) {
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));

        FeatureFlag previousSnapshot = new FeatureFlag(flag.getFlagKey(), flag.getName(), flag.getDescription(), flag.getFlagType());
        previousSnapshot.setArchived(flag.isArchived());

        flag.setArchived(true);
        FeatureFlag savedFlag = featureFlagRepository.save(flag);
        log.info("Archived feature flag [{}]", flagKey);

        auditLogService.recordMutation(flagKey, FLAG_ARCHIVED, performedBy, previousSnapshot, savedFlag);
        return savedFlag;
    }
}
