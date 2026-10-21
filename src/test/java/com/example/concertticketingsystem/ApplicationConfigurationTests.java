package com.example.concertticketingsystem;

import java.time.Clock;
import java.time.ZoneId;

import com.example.concertticketingsystem.config.ClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernateProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationConfigurationTests {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(BindingConfiguration.class, ClockConfig.class)
            .withPropertyValues("MYSQL_USER=config-test", "MYSQL_PASSWORD=test-only",
                    "MYSQL_HOST=db-test", "MYSQL_PORT=13306", "MYSQL_DATABASE=config_test");

    @Test
    void loadsAndBindsConfigurationWithoutConnectingToDatabase() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            DataSourceProperties datasource = context.getBean(DataSourceProperties.class);
            assertThat(datasource.getUrl()).isEqualTo("jdbc:mysql://db-test:13306/config_test");
            assertThat(datasource.getUsername()).isEqualTo("config-test");
            assertThat(datasource.getPassword()).isEqualTo("test-only");
            assertThat(context.getBean(HibernateProperties.class).getDdlAuto()).isEqualTo("validate");

            FlywayProperties flyway = context.getBean(FlywayProperties.class);
            assertThat(flyway.isEnabled()).isTrue();
            assertThat(flyway.getLocations()).containsExactly("classpath:db/migration");
            assertThat(flyway.isValidateOnMigrate()).isTrue();
            assertThat(flyway.isValidateMigrationNaming()).isTrue();
            assertThat(flyway.isBaselineOnMigrate()).isFalse();
            assertThat(flyway.isOutOfOrder()).isFalse();
            assertThat(flyway.isCleanDisabled()).isTrue();
            assertThat(context.getEnvironment().getProperty("management.endpoints.web.exposure.include"))
                    .isEqualTo("health");
            assertThat(context.getEnvironment().getProperty("management.endpoint.health.show-details"))
                    .isEqualTo("never");
            assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("Europe/Paris"));
        });
    }

    @Test
    void acceptsDatasourceAndApplicationOverrides() {
        runner.withPropertyValues("SPRING_DATASOURCE_URL=jdbc:mysql://override:3306/override_db",
                        "APP_TIME_ZONE=UTC")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(DataSourceProperties.class).getUrl())
                            .isEqualTo("jdbc:mysql://override:3306/override_db");
                    assertThat(context.getBean(Clock.class).getZone()).isEqualTo(ZoneId.of("UTC"));
                });
    }

    @Test
    void rejectsInvalidApplicationTimeZone() {
        runner.withPropertyValues("APP_TIME_ZONE=invalid-zone")
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({DataSourceProperties.class, FlywayProperties.class, HibernateProperties.class})
    static class BindingConfiguration {
    }
}
