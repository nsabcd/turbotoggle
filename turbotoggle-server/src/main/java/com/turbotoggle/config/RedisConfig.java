package com.turbotoggle.config;

import com.turbotoggle.infrastructure.RedisMutationPublisher;
import com.turbotoggle.infrastructure.messaging.redis.RedisMutationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 1.Flag Mutated in Control Plane:
 *      Admin API.
 *      An admin updates a flag rule. The domain fires a FlagConfigUpdatedEvent.
 * 2.Published via RedisMutationPublisher:
 *      Redis convertAndSend.
 *      The publisher converts the event to JSON and broadcasts it to channel flag-mutations:{sdkKey}.
 * 3.RedisMessageListenerContainer Catches Message:
 *      Background Redis Thread.
 *      The container routes the message to RedisMutationSubscriber.onMessage().
 * 4.Local Eviction & SSE Fan-Out:
 *      Data Plane.
 *      The subscriber invalidates local cache entries and prepares to stream the updated flag payload to connected SDK SSE streams.
 */

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Use strings for Redis keys and hash keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use Jackson for JSON serialization of the values (Payload DTOs)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    /**
     * Listens asynchronously to Redis Pub/Sub topics.
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisMutationSubscriber subscriber){
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(subscriber, new PatternTopic("flag-mutations:*"));
        return container;
    }
}
