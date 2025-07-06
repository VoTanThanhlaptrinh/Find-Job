package com.job_web.dto;

public interface JobResponse {
    Long getId();
    String getTitle();
    String getDescription();
    String getAddress();
    double getSalary();
    String getTime();
    int getApplies();
}