package com.turbotoggle.controller;

import com.turbotoggle.domain.entity.EnvironmentConfig;
import com.turbotoggle.domain.entity.FeatureFlag;
import com.turbotoggle.domain.model.CreateFlagRequestDto;
import com.turbotoggle.domain.model.UpdateConfigRulesRequestDto;
import com.turbotoggle.service.FetchFlagService;
import com.turbotoggle.service.FlagManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vi/control/flags")
public class FeatureFlagController {
    public final FlagManagementService flagManagementService;

    public FeatureFlagController(FlagManagementService flagManagementService) {
        this.flagManagementService = flagManagementService;
    }

    @PostMapping
    public ResponseEntity<FeatureFlag> createFlag(@Valid @RequestBody CreateFlagRequestDto request){
        FeatureFlag flag = flagManagementService.createFlag(request);
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
            @RequestBody UpdateConfigRulesRequestDto request
            ){
        EnvironmentConfig updatedConfig = flagManagementService.updateConfigRules(envKey, flagKey, request);
        return ResponseEntity.ok(updatedConfig);
    }

    @DeleteMapping("/{flagKey}")
    public ResponseEntity<FeatureFlag> archiveFlag(@PathVariable String flagKey) {
        FeatureFlag archivedFlag = flagManagementService.archiveFlag(flagKey);
        return ResponseEntity.ok(archivedFlag);
    }
}
