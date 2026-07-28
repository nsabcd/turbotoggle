package com.turbotoggle.server.infrastructure.sse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {
    public static final long EMITTER_TIME_OUT_MS = 30*60*1000L;
    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<String , List<SseEmitter>> emittersPerSdkKey = new ConcurrentHashMap<>();

    public SseEmitter register(String sdkKey){
        SseEmitter emitter = new SseEmitter(EMITTER_TIME_OUT_MS);
        List<SseEmitter> emitters = emittersPerSdkKey.computeIfAbsent(sdkKey, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        //cleanup
        emitter.onCompletion(() -> remove(sdkKey, emitter));
        emitter.onTimeout(() -> remove(sdkKey, emitter));
        emitter.onError(e -> remove(sdkKey, emitter));
        return emitter;
    }

    public void broadcast(String sdkKey, String eventName, Object data){
        List<SseEmitter> emitters = emittersPerSdkKey.get(sdkKey);
        if(emitters==null || emitters.isEmpty()){
            return;
        }
        log.debug("Broadcasting event [{}] to {} clients for sdkKey [{}]", eventName, emitters.size(), sdkKey);
        for(SseEmitter emitter : emitters){
            try{
                emitter.send(SseEmitter.event().name(eventName).data(data));
            }catch (IOException e){
                log.warn("Failed to send SSE event to client. Evicting emitter.");
                remove(sdkKey, emitter);
            }

        }
    }

    public void remove(String sdkKey, SseEmitter emitter){
        List<SseEmitter> emitters = emittersPerSdkKey.get(sdkKey);
        if(emitters!=null){
            emitters.remove(emitter);
            if(emitters.isEmpty()){
                emittersPerSdkKey.remove(sdkKey);
            }
        }
    }
}
