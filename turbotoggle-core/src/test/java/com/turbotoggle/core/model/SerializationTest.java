package com.turbotoggle.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.turbotoggle.core.model.Operator.EQUALS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("FlagConfigPayloadDto should correctly serialize and deserialize to/from JSON")
    void shouldSerializeAndDeserializeFlagConfigPayloadDto() throws Exception{
        Clause clause = new Clause(
                "country",
                EQUALS,
                List.of("US", "Canada")
        );
        PercentageRollout rollout = new PercentageRollout("variation-a", 100);
        TargetingRule rule = new TargetingRule(
                "rule-1",
                null,
                List.of(clause),
                List.of(rollout)
        );

        FlagConfigPayloadDto original = new FlagConfigPayloadDto(
                "rule-1",
                "BOOLEAN",
                true,
                "false",
                List.of(rule),
                Map.of("user-1", "true"),
                1L
        );

        String json = objectMapper.writeValueAsString(original);
        assertNotNull(json);

        FlagConfigPayloadDto deserialized = objectMapper.readValue(json, FlagConfigPayloadDto.class);
        assertEquals(original.flagKey(), deserialized.flagKey());
        assertEquals(original.enabled(), deserialized.enabled());
        assertEquals(original.rules().size(), deserialized.rules().size());
        assertEquals("country", deserialized.rules().get(0).getClauses().get(0).getAttribute());
        assertEquals(100, deserialized.rules().get(0).getPercentageRollouts().get(0).getPercentage());
    }
}
