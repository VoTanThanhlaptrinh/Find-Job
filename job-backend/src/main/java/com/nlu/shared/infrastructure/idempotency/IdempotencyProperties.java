package com.nlu.shared.infrastructure.idempotency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties {

    private boolean enabled = true;
    private String headerName = IdempotencyConstants.HEADER_IDEMPOTENCY_KEY;
    private long lockTtlSeconds = 60;
    private long completedTtlSeconds = 86_400;
    private List<String> endpoints = new ArrayList<>(IdempotencyConstants.DEFAULT_ENDPOINTS);
}

