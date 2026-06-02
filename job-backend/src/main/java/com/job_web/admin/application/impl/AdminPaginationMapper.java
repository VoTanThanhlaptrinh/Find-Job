package com.job_web.admin.application.impl;

import com.job_web.shared.domain.model.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class AdminPaginationMapper {

    public PageResponse.Pagination map(int page, int pageSize, Page<?> result) {
        return PageResponse.Pagination.builder()
                .page(page)
                .pageSize(pageSize)
                .totalItems(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }
}
