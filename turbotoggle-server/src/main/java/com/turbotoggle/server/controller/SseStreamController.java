package com.turbotoggle.server.controller;

import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.server.infrastructure.sse.SseEmitterRegistry;
import com.turbotoggle.server.service.FetchFlagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stream")
public class SseStreamController {
    private static final Logger log = LoggerFactory.getLogger(SseStreamController.class);
    private final SseEmitterRegistry sseEmitterRegistry;
    private final FetchFlagService fetchFlagService;

    public SseStreamController(SseEmitterRegistry sseEmitterRegistry, FetchFlagService fetchFlagService) {
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.fetchFlagService = fetchFlagService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @RequestHeader(value="X-SDK-Key", required = false) String headerSdkKey,
            @RequestParam(value = "sdkKey", required = false) String paramSdkKey
    ){
        String sdkKey = headerSdkKey!= null? headerSdkKey:paramSdkKey;
        if(sdkKey==null || sdkKey.isBlank()){
            log.error("SDK Key is null or empty.");
            throw new IllegalArgumentException("SDK Key is required via 'X-SDK-Key' header or 'sdkKey' query parameter.");
        }

        //validate sdkKey
        fetchFlagService.getEnvKeyBySdkKey(sdkKey);

        SseEmitter emitter = sseEmitterRegistry.register(sdkKey);

        // Send initial full payload snapshot immediately upon connection
        try{
            log.info("Sending initial full payload snapshot for env [{}].",sdkKey);
            List<FlagConfigPayloadDto> initialFlags = fetchFlagService.getAllFlagsForSdk(sdkKey);
            emitter.send(SseEmitter.event().name("INIT").data(initialFlags));
        }catch (IOException e){
            log.error("Emitter error while sending initial payload for env[{}]",sdkKey);
            emitter.completeWithError(e);
        }
        return emitter;
    }
}
