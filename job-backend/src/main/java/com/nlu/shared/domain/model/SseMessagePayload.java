package com.nlu.shared.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessagePayload<T> {
    private long id;
    private String status;
    private String message;
    private T data;
}