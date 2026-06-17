package ru.copperside.paylimits.projection.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pay-limit-projection.internal-admin")
public record InternalAdminKeyProperties(String apiKey, boolean required) {
}
