package com.turbotoggle.infrastructure.messaging.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.infrastructure.sse.SseEmitterRegistry;
import com.turbotoggle.repository.Cache.FlagCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class RedisMutationSubscriber implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(RedisMutationSubscriber.class);

    private final ObjectMapper objectMapper;
    private final FlagCacheRepository flagCacheRepository;
    private final SseEmitterRegistry sseEmitterRegistry;

    public RedisMutationSubscriber(ObjectMapper objectMapper, FlagCacheRepository flagCacheRepository, SseEmitterRegistry sseEmitterRegistry) {
        this.objectMapper = objectMapper;
        this.flagCacheRepository = flagCacheRepository;
        this.sseEmitterRegistry=sseEmitterRegistry;

    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try{
            FlagConfigUpdatedEvent event = objectMapper.readValue(message.getBody(), FlagConfigUpdatedEvent.class);
            String channel = new String(message.getChannel());
            log.info("Received flag mutation event on channel [{}]: flag [{}] in env [{}]",
                    channel, event.updatedFlag().flagKey(), event.sdkKey());

            //Evict local/distributed cache so fresh values are read
            flagCacheRepository.evictEnvironment(event.sdkKey());

            //Broadcast real-time SSE update event to connected SDKs
            sseEmitterRegistry.broadcast(event.sdkKey(), "FLAG_MUTATED", event);
        }catch (IOException e) {
            log.error("Failed to deserialize FlagConfigUpdatedEvent payload", e);
        }
    }
}
