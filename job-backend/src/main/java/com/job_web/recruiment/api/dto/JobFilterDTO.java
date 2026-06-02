package com.job_web.recruiment.api.dto;

import com.job_web.recruiment.domain.vo.EmploymentType;

import java.util.List;

public record JobFilterDTO(
        int pageIndex,
        int pageSize,
        int min,
        int max,
        List<String> address,
        List<EmploymentType> times,
        String title
) {
    public int getPageIndex() {
        return pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public List<String> getAddress() {
        return address;
    }

    public List<EmploymentType> getTimes() {
        return times;
    }
    public String getTitle(){ return title;}
}
