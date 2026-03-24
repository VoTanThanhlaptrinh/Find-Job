package com.job_web.dto.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JobApply {
    private final Long id;
    private final String title;
    private final String description;
    private final String address;
    private final double salary;
    private final String time;
}
