package com.nlu.shared.infrastructure.idempotency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    private IdempotencyStatus status;
    private String requestHash;
    private String lockToken;
    private int httpStatus;
    private String contentType;
    private String responseBody;
    private long createdAtEpochMs;
}

