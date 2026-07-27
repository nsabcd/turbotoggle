package com.turbotoggle.service;

import com.turbotoggle.domain.entity.Environment;
import com.turbotoggle.domain.entity.EnvironmentConfig;
import com.turbotoggle.domain.entity.FeatureFlag;
import com.turbotoggle.domain.enums.FlagType;
import com.turbotoggle.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.domain.model.CreateFlagRequestDto;
import com.turbotoggle.domain.model.FlagConfigPayloadDto;
import com.turbotoggle.domain.model.UpdateConfigRulesRequestDto;
import com.turbotoggle.infrastructure.RedisMutationPublisher;
import com.turbotoggle.repository.EnvironmentConfigRepository;
import com.turbotoggle.repository.FeatureFlagRepository;

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

    public FlagManagementService(
            FeatureFlagRepository featureFlagRepository,
            EnvironmentConfigRepository environmentConfigRepository,
            EnvironmentManagementService environmentManagementService,
            RedisMutationPublisher mutationPublisher) {
        this.featureFlagRepository = featureFlagRepository;
        this.environmentConfigRepository = environmentConfigRepository;
        this.environmentManagementService = environmentManagementService;
        this.mutationPublisher=mutationPublisher;
    }

    public FeatureFlag createFlag(CreateFlagRequestDto request){
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

    public EnvironmentConfig updateConfigRules(String envKey, String flagKey, UpdateConfigRulesRequestDto request){
        Environment env = environmentManagementService.getByEnvKey(envKey);

        EnvironmentConfig config = environmentConfigRepository.findByEnvKeyAndFlagKey(envKey, flagKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Config not found for flag [%s] in env [%s]", flagKey, envKey)));

        //update the fields
        if(request.enabled() != null) config.setEnabled(request.enabled());
        if(request.defaultVariation() != null) config.setDefaultVariation(request.defaultVariation());
        if (request.rules() != null) config.setRules(request.rules());
        if (request.individualTargets() != null) config.setIndividualTargets(request.individualTargets());

        // Increment version for optimistic locking & tracking
        config.setVersion(config.getVersion() + 1);

        EnvironmentConfig savedConfig = environmentConfigRepository.save(config);
        log.info("Updated rules for flag [{}] in env [{}], new version [{}]", flagKey, envKey, savedConfig.getVersion());

        FlagConfigPayloadDto payloadDto = new FlagConfigPayloadDto(
                flagKey,
                savedConfig.getFeatureFlag().getFlagType().toString(),
                savedConfig.isEnabled(),
                savedConfig.getDefaultVariation(),
                new ArrayList<>(savedConfig.getRules()),
                savedConfig.getIndividualTargets(),
                savedConfig.getVersion()
        );

        FlagConfigUpdatedEvent event = new FlagConfigUpdatedEvent(
                env.getSdkKey(),
                flagKey,
                savedConfig.getVersion(),
                payloadDto);

        mutationPublisher.publishMutation(event);

        return savedConfig;
    }
    @Transactional(readOnly = true)
    public List<FeatureFlag> getAllFlags() {
        return featureFlagRepository.findAll();
    }

    public FeatureFlag archiveFlag(String flagKey) {
        FeatureFlag flag = featureFlagRepository.findByFlagKey(flagKey)
                .orElseThrow(() -> new IllegalArgumentException("Flag not found: " + flagKey));

        flag.setArchived(true);
        log.info("Archived feature flag [{}]", flagKey);
        return featureFlagRepository.save(flag);
    }
}
