package com.turbotoggle.controller;

import com.turbotoggle.domain.entity.Environment;
import com.turbotoggle.domain.model.CreateEnvironmentRequestDto;
import com.turbotoggle.service.EnvironmentManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("api/v1/environments")
public class EnvironmentController {
    private final EnvironmentManagementService environmentManagementService;

    public EnvironmentController(EnvironmentManagementService environmentManagementService) {
        this.environmentManagementService = environmentManagementService;
    }

    @PostMapping
    public ResponseEntity<Environment> createEnvironment(@Valid @RequestBody CreateEnvironmentRequestDto request){
        Environment env = environmentManagementService.createEnvironment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(env);
    }

    @GetMapping
    public ResponseEntity<List<Environment>> getAllEnvironments(){
        return ResponseEntity.ok(environmentManagementService.getAllEnvironments());
    }

    @GetMapping("/{envKey}")
    public ResponseEntity<Environment> getEnvironment(@PathVariable String envKey){
        return ResponseEntity.ok(environmentManagementService.getByEnvKey(envKey));
    }

    @DeleteMapping("/{envKey}")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable String envKey) {
        environmentManagementService.deleteEnvironment(envKey);
        return ResponseEntity.noContent().build();
    }

}
