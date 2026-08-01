package com.turbotoggle.server.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.FlagConfigPayloadDto;
import com.turbotoggle.core.model.Operator;
import com.turbotoggle.core.model.TargetingRule;
import com.turbotoggle.server.domain.event.FlagConfigUpdatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class) // <-- Ensures Spring injects @Autowired fields
@JsonTest
public class FlagPayloadJsonTest {
    @Autowired
    ObjectMapper objectMapper;

    @Test
    @DisplayName("Should correctly serialize and deserialize FlagConfigPayloadDto DTO")
    void shouldSerializeAndDeserializeFlagConfigPayloadDto() throws Exception {
        Clause clause = new Clause("country", Operator.EQUALS, List.of("US", "CA"));
        TargetingRule rule = new TargetingRule("rule-1", "variant-a", List.of(clause), List.of());

        FlagConfigPayloadDto original = new FlagConfigPayloadDto(
                "checkout-flag",
                "BOOLEAN",
                true,
                "false",
                List.of(rule),
                Map.of("user-1", "true"),
                5L
        );

        String json = objectMapper.writeValueAsString(original);
        FlagConfigPayloadDto deserialized = objectMapper.readValue(json, FlagConfigPayloadDto.class);

        assertThat(deserialized.flagKey()).isEqualTo(original.flagKey());
        assertThat(deserialized.flagType()).isEqualTo(original.flagType());
        assertThat(deserialized.enabled()).isEqualTo(original.enabled());
        assertThat(deserialized.defaultVariation()).isEqualTo(original.defaultVariation());
        assertThat(deserialized.rules().get(0).getVariation()).isEqualTo(original.rules().get(0).getVariation());
        assertThat(deserialized.individualTargets().get("user-1")).isEqualTo(original.individualTargets().get("user-1"));

    }

    @Test
    @DisplayName("Should correctly serialize and deserialize FlagConfigUpdatedEvent for Redis PubSub")
    void shouldSerializeAndDeserializeFlagConfigUpdatedEvent() throws Exception {
        FlagConfigPayloadDto payload = new FlagConfigPayloadDto(
                "checkout-flag", "BOOLEAN", true, "false", List.of(), Map.of(), 1L
        );
        FlagConfigUpdatedEvent event = new FlagConfigUpdatedEvent("sdk-123", "checkout-flag", 1L, payload);

        String json = objectMapper.writeValueAsString(event);
        FlagConfigUpdatedEvent deserialized = objectMapper.readValue(json, FlagConfigUpdatedEvent.class);

        assertThat(deserialized.sdkKey()).isEqualTo("sdk-123");
        assertThat(deserialized.updatedFlag().flagKey()).isEqualTo("checkout-flag");
    }
}
