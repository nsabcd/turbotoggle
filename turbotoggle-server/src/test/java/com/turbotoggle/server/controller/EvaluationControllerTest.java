package com.turbotoggle.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.core.model.EvaluationResultDto;
import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.core.service.EvaluatorService;
import com.turbotoggle.server.service.FetchFlagService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluationController.class)
public class EvaluationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FetchFlagService fetchFlagService;

    @MockBean
    private EvaluatorService evaluatorService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/evaluate - Success path")
    void evaluate_Success() throws Exception {
        // Arrange: Mock EvaluatorService response
        when(evaluatorService.evaluate(any(), any())).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/v1/evaluate")
                        .header("X-SDK-Key", "test-sdk-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "flagKey": "my-feature-flag",
                        "entityId": "user-123"
                    }
                    """))
                .andExpect(status().isOk());
    }
    @Test
    @DisplayName("Evaluate Batch - Full Snapshot (No flag keys provided)")
    void evaluateBatch_FullSnapshot_Success() throws Exception {
        // Mock DB / Cache returns 1 flag config
        FlagConfigPayloadDto mockConfig = new FlagConfigPayloadDto(
                "flag-1",
                "BOOLEAN",
                true,
                "true",
                List.of(),
                Map.of(),
                1L
        );
        EvaluationResultDto mockResult = new EvaluationResultDto("flag-1", true, "MATCH", "RULE_MATCH", 1L);

        when(fetchFlagService.getAllFlagsForSdk("test-sdk-key")).thenReturn(List.of(mockConfig));
        when(evaluatorService.evaluate(eq(mockConfig), any())).thenReturn(mockResult);

        mockMvc.perform(post("/api/v1/evaluate")
                        .header("X-SDK-Key", "test-sdk-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Empty request triggers full snapshot
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.flag-1.flagKey").value("flag-1"));
    }

    @Test
    @DisplayName("Evaluate Batch - Specific Flag Keys (Includes missing flag handling)")
    void evaluateBatch_SpecificKeysWithMissingFlag_Success() throws Exception {
        // Request flags: "existing-flag" and "unknown-flag"
        FlagConfigPayloadDto mockConfig = new FlagConfigPayloadDto(
                "existing-flag",
                "BOOLEAN",
                true,
                "true",
                List.of(),
                Map.of(),
                1L
        );
        EvaluationResultDto mockResult = new EvaluationResultDto("existing-flag", true, "MATCH", "RULE_MATCH", 1L);

        // Fetch flag configs returns only "existing-flag"
        when(fetchFlagService.getFlagConfigs(eq("test-sdk-key"), eq(List.of("existing-flag", "unknown-flag"))))
                .thenReturn(List.of(mockConfig));
        when(evaluatorService.evaluate(eq(mockConfig), any())).thenReturn(mockResult);

        String jsonBody = """
                {
                  "flagKeys": ["existing-flag", "unknown-flag"],
                  "context": {}
                }
                """;

        mockMvc.perform(post("/api/v1/evaluate")
                        .param("sdkKey", "test-sdk-key") // Test query parameter path for sdkKey
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flags.existing-flag.flagKey").value("existing-flag"))
                // Verifies the putIfAbsent branch for unknown flags!
                .andExpect(jsonPath("$.flags.unknown-flag.reason").value("FLAG_NOT_FOUND"));
    }

    @Test
    @DisplayName("Evaluate Batch - Missing SDK Key throws IllegalArgumentException")
    void evaluateBatch_MissingSdkKey_ThrowsException() throws Exception {
        mockMvc.perform(post("/api/v1/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest()); // Assuming exception handler turns IllegalArgumentException into 400
    }
}
