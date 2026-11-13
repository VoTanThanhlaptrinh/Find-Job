package com.nlu.identity.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties under {@code app.user-details-cache} for caching authenticated
 * user principals in Redis to avoid DB lookups in {@code JwtFilter}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.user-details-cache")
public class UserDetailsCacheProperties {

    private boolean enabled = true;
    private long ttlSeconds = 300L;
}

