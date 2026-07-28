package com.turbotoggle.server.service;

import com.turbotoggle.server.domain.entity.Environment;
import com.turbotoggle.server.domain.model.CreateEnvironmentRequestDto;
import com.turbotoggle.server.repository.EnvironmentRepository;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EnvironmentManagementService {
    private static final Logger log = LoggerFactory.getLogger(EnvironmentManagementService.class);

    private final EnvironmentRepository environmentRepository;

    public EnvironmentManagementService(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    public Environment createEnvironment(CreateEnvironmentRequestDto request){
        if(environmentRepository.findByEnvKey(request.envKey()).isPresent()){
            throw new IllegalArgumentException("Environment key already exists: " + request.envKey());
        }

        String sdkKey = "sdk_" +  UUID.randomUUID().toString().replace("-", "");

        Environment environment = new Environment();
        environment.setEnvKey(request.envKey());
        environment.setName(request.name());
        environment.setSdkKey(sdkKey);

        log.info("Creating new environment [{}] with SDK Key [{}]", request.envKey(), sdkKey);
        return environmentRepository.save(environment);
    }

    @Transactional(readOnly = true)
    public List<Environment> getAllEnvironments() {
        return environmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Environment getByEnvKey(String envKey) {
        return environmentRepository.findByEnvKey(envKey)
                .orElseThrow(() -> new IllegalArgumentException("Environment not found: " + envKey));
    }

    public void deleteEnvironment(String envKey) {
        Environment env = getByEnvKey(envKey);
        environmentRepository.delete(env);
        log.info("Deleted environment [{}]", envKey);
    }
}
