package com.turbotoggle.server.migration;

import com.turbotoggle.core.model.Clause;
import com.turbotoggle.core.model.Operator;
import com.turbotoggle.core.model.TargetingRule;
import com.turbotoggle.server.domain.entity.Environment;
import com.turbotoggle.server.domain.entity.EnvironmentConfig;
import com.turbotoggle.server.domain.entity.FeatureFlag;
import com.turbotoggle.server.domain.enums.FlagType;
import com.turbotoggle.server.repository.EnvironmentConfigRepository;
import com.turbotoggle.server.repository.EnvironmentRepository;
import com.turbotoggle.server.repository.FeatureFlagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FlywayMigrationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private FeatureFlagRepository featureFlagRepository;

    @Autowired
    private EnvironmentRepository environmentRepository;

    @Autowired
    private EnvironmentConfigRepository environmentConfigRepository;

   // @Test
    void shouldRunFlywayMigrationsAndPersistJsonbEntities(){
        // 1. Create Feature Flag
        FeatureFlag flag = new FeatureFlag();
        flag.setFlagKey("beta-dashboard");
        flag.setName("Beta Dashboard UI");
        flag.setFlagType(FlagType.BOOLEAN);
        flag = featureFlagRepository.save(flag);

        // 2. Create Environment
        Environment env = new Environment();
        env.setEnvKey("production");
        env.setName("Production");
        env.setSdkKey("sdk_prod_12345");
        env = environmentRepository.save(env);

        // 3. Create EnvironmentConfig with complex JSONB rules
        EnvironmentConfig config = new EnvironmentConfig();
        config.setFeatureFlag(flag);
        config.setEnvironment(env);
        config.setEnabled(true);
        config.setDefaultVariation("false");

        TargetingRule rule = new TargetingRule();
        rule.setRuleId("rule-1");
        rule.setVariation("true");
        rule.setClauses(List.of(new Clause("email", Operator.ENDS_WITH, List.of("@company.com"))));

        config.setRules(List.of(rule));
        config.setIndividualTargets(Map.of("user-99", "true"));

        EnvironmentConfig savedConfig = environmentConfigRepository.save(config);

        // 4. Assert Persistence & Deserialization from PostgreSQL
        assertThat(savedConfig.getId()).isNotNull();

        EnvironmentConfig retrieved = environmentConfigRepository.findById(savedConfig.getId()).orElseThrow();
        assertThat(retrieved.getRules()).hasSize(1);
        assertThat(retrieved.getRules().get(0).getRuleId()).isEqualTo("rule-1");
        assertThat(retrieved.getIndividualTargets()).containsEntry("user-99", "true");

    }


}
