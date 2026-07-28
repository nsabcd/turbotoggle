package com.turbotoggle.server.controller;

import com.turbotoggle.server.domain.entity.EnvironmentConfig;
import com.turbotoggle.server.domain.entity.FeatureFlag;
import com.turbotoggle.server.domain.model.CreateFlagRequestDto;
import com.turbotoggle.server.domain.model.UpdateConfigRulesRequestDto;
import com.turbotoggle.server.service.FlagManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/control/flags")
public class FeatureFlagController {
    public final FlagManagementService flagManagementService;

    public FeatureFlagController(FlagManagementService flagManagementService) {
        this.flagManagementService = flagManagementService;
    }

    @PostMapping
    public ResponseEntity<FeatureFlag> createFlag(@Valid @RequestBody CreateFlagRequestDto request,
                                                  @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId){
        FeatureFlag flag = flagManagementService.createFlag(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(flag);
    }

    @GetMapping
    public ResponseEntity<List<FeatureFlag>> getAllFlags(){
        return ResponseEntity.ok(flagManagementService.getAllFlags());
    }

    @PutMapping("/{envKey}/{flagKey}/rules")
    public ResponseEntity<EnvironmentConfig> updateRules(
            @PathVariable String envKey,
            @PathVariable String flagKey,
            @RequestBody UpdateConfigRulesRequestDto request,
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId
            ){
        EnvironmentConfig updatedConfig = flagManagementService.updateConfigRules(envKey, flagKey, request, userId);
        return ResponseEntity.ok(updatedConfig);
    }

    @DeleteMapping("/{flagKey}")
    public ResponseEntity<FeatureFlag> archiveFlag(@PathVariable String flagKey, @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId) {
        FeatureFlag archivedFlag = flagManagementService.archiveFlag(flagKey, userId);
        return ResponseEntity.ok(archivedFlag);
    }
}
