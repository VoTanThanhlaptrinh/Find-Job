package com.job_web.dto.job;

import java.util.List;

public record PagedPayload<T>(List<T> content) {
}
