package ru.copperside.paylimits.projection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:projection-startup",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "pay-limit-projection.kafka.enabled=false"
})
@ActiveProfiles("local")
class PayLimitProjectionApplicationTests {

    @Test
    void contextLoads() {
    }
}
