package com.turbotoggle.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.domain.event.FlagConfigUpdatedEvent;
import com.turbotoggle.service.cache.FlagMutationPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMutationPublisher implements FlagMutationPublisher {
    public static final String FLAG_MUTATIONS_TOPIC = "flag-mutations:global";
    public static final String FLAG_MUTATIONS_CHANNEL_PREFIX = "flag-mutations:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisMutationPublisher(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper=objectMapper;
    }

    @Override
    public void publishMutation(FlagConfigUpdatedEvent event){
        try{
            String jsonPayload = objectMapper.writeValueAsString(event);
            // Allows listeners/SSE instances to subscribe ONLY to updates for a specific environment
            String channel = FLAG_MUTATIONS_CHANNEL_PREFIX + event.sdkKey();
            redisTemplate.convertAndSend(channel, event);
            // Optional: Also send to a global channel if audit logs/global metrics need it
            redisTemplate.convertAndSend(FLAG_MUTATIONS_TOPIC, event);
        }catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize FlagConfigUpdatedEvent", e);
        }
    }
}
