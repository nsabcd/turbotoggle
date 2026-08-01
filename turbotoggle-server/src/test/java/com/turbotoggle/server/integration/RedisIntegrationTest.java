package com.turbotoggle.server.integration;

import com.turbotoggle.server.config.TestRedisConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class RedisIntegrationTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void testRedisConnection() {
        redisTemplate.opsForValue().set("testKey", "hello-turbo");
        String value = redisTemplate.opsForValue().get("testKey");
        assertThat(value).isEqualTo("hello-turbo");
    }
}
