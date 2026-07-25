package com.turbotoggle.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.turbotoggle.domain.model.FlagConfigPayloadDto;
import com.turbotoggle.repository.EnvironmentConfigRepository;
import com.turbotoggle.repository.projection.FlagPayloadProjection;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.beans.Transient;
import java.util.List;
import java.util.Map;

@Service
public class FetchFlagService {
    private final EnvironmentConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    public FetchFlagService(EnvironmentConfigRepository configRepository, ObjectMapper objectMapper) {
        this.configRepository = configRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<FlagConfigPayloadDto> getActiveFlagsForSdk(String sdkKey){
        List<FlagPayloadProjection> projections = configRepository.findAllActiveFlagPayloadsBySdkKey(sdkKey);

        return projections.stream().map((FlagPayloadProjection p) -> {
            try{
                List<Object> rules = objectMapper.readValue(p.getRulesJson(), List.class);
                Map<String, String> targets = objectMapper.readValue(p.getIndividualTargetsJson(), Map.class);
                return new FlagConfigPayloadDto(
                        p.getFlagKey(),
                        p.getFlagType(),
                        Boolean.TRUE.equals(p.getEnabled()),
                        p.getDefaultVariation(),
                        rules,
                        targets,
                        p.getVersion()
                );
            }catch(JsonProcessingException e){
                throw new IllegalStateException("Failed to parse JSON payload for flag: "+p.getFlagKey(), e);
            }
        }).toList();
    }


}
