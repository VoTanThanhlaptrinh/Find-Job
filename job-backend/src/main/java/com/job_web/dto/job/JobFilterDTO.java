package com.job_web.dto.job;

import java.util.List;

public record JobFilterDTO(
        int pageIndex,
        int pageSize,
        int min,
        int max,
        List<String> address,
        List<String> times,
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

    public List<String> getTimes() {
        return times;
    }
    public String getTitle(){ return title;}
}
