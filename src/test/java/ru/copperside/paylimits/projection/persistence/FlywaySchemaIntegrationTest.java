package ru.copperside.paylimits.projection.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("pgtest")
class FlywaySchemaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl() + "&currentSchema=pay_limit_projection");
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void createsProjectionTables() {
        Integer events = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'pay_limit_projection' and table_name = 'reservation_event'",
                Integer.class);
        Integer state = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'pay_limit_projection' and table_name = 'reservation_state'",
                Integer.class);
        assertThat(events).isEqualTo(1);
        assertThat(state).isEqualTo(1);
    }
}
