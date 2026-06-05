package com.nlu.shared.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private Pagination pagination;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Pagination {
        private int page;
        private int pageSize;
        private long totalItems;
        private int totalPages;
    }
}
