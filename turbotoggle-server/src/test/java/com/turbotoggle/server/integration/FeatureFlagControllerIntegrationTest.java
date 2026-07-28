package com.turbotoggle.server.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.PercentageRollout;
import com.turbotoggle.core.model.TargetingRule;
import com.turbotoggle.server.domain.entity.Environment;
import com.turbotoggle.server.domain.enums.FlagType;
import com.turbotoggle.server.domain.model.CreateEnvironmentRequestDto;
import com.turbotoggle.server.domain.model.CreateFlagRequestDto;
import com.turbotoggle.server.domain.model.UpdateConfigRulesRequestDto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Equals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static com.turbotoggle.core.model.Operator.EQUALS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FeatureFlagControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Full Lifecycle: Create Env -> Create Flag -> Update Rules -> Verify Response")
    void shouldPerformFullFlagLifecycle() throws Exception {
        CreateEnvironmentRequestDto envReq = new CreateEnvironmentRequestDto(
                "staging",
                "Staging Environment"
        );

        mockMvc.perform(
                post("/api/v1/environments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.envKey").value("staging")
        );

        CreateFlagRequestDto flagReq = new CreateFlagRequestDto(
                "checkout-v2",
                "New Checkout Flow",
                "Beta Checkout UI",
                FlagType.BOOLEAN
        );

        mockMvc.perform(
                post("/api/v1/control/flags")
                        .header("X-User-Id", "admin@turbotoggle.io")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(flagReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flagKey").value("checkout-v2"));

        Clause clause = new Clause("country", EQUALS, List.of("US"));
        TargetingRule rule = new TargetingRule("rule-1", "true", List.of(clause), List.of());
        UpdateConfigRulesRequestDto updateReq = new UpdateConfigRulesRequestDto(true, "false", List.of(rule), Map.of("user-10", "true"));
        mockMvc.perform(put("/api/v1/control/flags/staging/checkout-v2/rules")
                        .header("X-User-Id", "admin@turbotoggle.io")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.defaultVariation").value("false"))
                .andExpect(jsonPath("$.version").value(1));

        clause = new Clause("country", EQUALS, List.of("US"));
        rule = new TargetingRule("rule-1", "true", List.of(clause), List.of());
        updateReq = new UpdateConfigRulesRequestDto(true, "false", List.of(rule), Map.of("user-11", "true"));
        mockMvc.perform(put("/api/v1/control/flags/staging/checkout-v2/rules")
                        .header("X-User-Id", "admin@turbotoggle.io")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.defaultVariation").value("false"))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("Should return 400 BAD_REQUEST when rule validation fails (percentage rollouts don't sum to 100%)")
    void shouldFailValidationForInvalidRollouts() throws Exception {
        // 1. Setup Environment & Flag
        mockMvc.perform(post("/api/v1/environments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateEnvironmentRequestDto("dev", "Development"))));

        mockMvc.perform(post("/api/v1/control/flags")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CreateFlagRequestDto("bad-rollout", "Bad Rollout Flag", "", FlagType.BOOLEAN))));

        // 2. Create invalid rule where percentages sum to 80% instead of 100%
        PercentageRollout invalidRollout = new PercentageRollout("true", 80);
        Clause clause = new Clause("key", "EQUALS", List.of("user-1"));
        TargetingRule invalidRule = new TargetingRule("rule-invalid", null, List.of(clause), List.of(invalidRollout));

        UpdateConfigRulesRequestDto invalidUpdateReq = new UpdateConfigRulesRequestDto(true, "false", List.of(invalidRule), Map.of());

        mockMvc.perform(put("/api/v1/control/flags/dev/bad-rollout/rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUpdateReq)))
                .andExpect(status().isBadRequest()) // ✅ Expect HTTP 400
                .andExpect(jsonPath("$.error").value("Target variant rollout percentages must sum to exactly 100%. Total was: 80.0"));
    }
}
