package com.job_web.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobFilterDTO {
    private int pageIndex;
    private int pageSize;
    private int min;
    private int max;
    private List<String> address;
    private List<String> times;
}
