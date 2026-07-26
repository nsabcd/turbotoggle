package com.turbotoggle.controller;

import com.turbotoggle.domain.model.*;
import com.turbotoggle.service.FetchFlagService;
import com.turbotoggle.service.evaluator.EvaluatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/evaluate")
public class EvaluationController {

    private final FetchFlagService fetchFlagService;
    private final EvaluatorService evaluatorService;

    public EvaluationController(FetchFlagService fetchFlagService, EvaluatorService evaluatorService) {
        this.fetchFlagService = fetchFlagService;
        this.evaluatorService = evaluatorService;
    }

    /**
     * Evaluates a single flag for a given context.
     * Example: POST /api/v1/evaluate/new-checkout-v2
     */
    @PostMapping("/{flagKey}")
    public ResponseEntity<EvaluationResultDto> evaluateSingleFlag(
            @RequestHeader(value = "X-SDK-Key", required = false) String headerSdkKey,
            @RequestParam(value = "sdkKey", required = false) String paramSdkKey,
            @PathVariable String flagKey,
            @Valid @RequestBody EvaluationContextDto context
    ) {
        String sdkKey = resolveSdkKey(headerSdkKey, paramSdkKey);

        // Fetch flag config via Cache-Aside service (Redis -> DB)
        FlagConfigPayloadDto flagConfig = fetchFlagService.getFlagConfig(sdkKey, flagKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("Flag [%s] not found for SDK key [%s]", flagKey, sdkKey)));

        // Evaluate context against targeting rules
        EvaluationResultDto result = evaluatorService.evaluate(flagConfig, context);

        return ResponseEntity.ok(result);
    }

    /**
     * Evaluates all flags (or a batch subset) for a given environment context.
     * Example: POST /api/v1/evaluate
     */
    @PostMapping
    public ResponseEntity<BatchEvaluationResponseDto> evaluateBatch(
            @RequestHeader(value = "X-SDK-Key", required = false) String headerSdkKey,
            @RequestParam(value = "sdkKey", required = false) String paramSdkKey,
            @Valid @RequestBody EvaluationRequestDto request
    ) {
        String sdkKey = resolveSdkKey(headerSdkKey, paramSdkKey);

        // Ensure environment exists
        fetchFlagService.getEnvKeyBySdkKey(sdkKey);

        List<FlagConfigPayloadDto> flagConfigs;

        if (request.flagKeys() != null && !request.flagKeys().isEmpty()) {
            // Bulk fetch requested keys
            flagConfigs = fetchFlagService.getFlagConfigs(sdkKey, request.flagKeys());
        } else {
            // Fetch full payload snapshot for environment
            flagConfigs = fetchFlagService.getAllFlagsForSdk(sdkKey);
        }

        Map<String, EvaluationResultDto> results = new HashMap<>();

        // Map existing flag evaluations
        for (FlagConfigPayloadDto flagConfig : flagConfigs) {
            EvaluationResultDto result = evaluatorService.evaluate(flagConfig, request.context());
            results.put(flagConfig.flagKey(), result);
        }

        // Handle requested flag keys that were NOT found in DB/Cache
        if (request.flagKeys() != null) {
            for (String requestedKey : request.flagKeys()) {
                results.putIfAbsent(requestedKey, new EvaluationResultDto(
                        requestedKey,
                        null,
                        "UNKNOWN_FLAG",
                        "FLAG_NOT_FOUND",
                        0L
                ));
            }
        }

        return ResponseEntity.ok(new BatchEvaluationResponseDto(results));
    }

    private String resolveSdkKey(String headerKey, String paramKey) {
        String sdkKey = (headerKey != null && !headerKey.isBlank()) ? headerKey : paramKey;
        if (sdkKey == null || sdkKey.isBlank()) {
            throw new IllegalArgumentException("SDK Key must be provided in 'X-SDK-Key' header or 'sdkKey' query parameter");
        }
        return sdkKey;
    }
}