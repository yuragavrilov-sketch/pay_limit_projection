package ru.copperside.paylimits.projection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PayLimitProjectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayLimitProjectionApplication.class, args);
    }
}
