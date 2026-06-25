package com.nlu.recruitment.api.dto.category;

import lombok.Data;

@Data
public class CategoryRequest {
    private String name;
    private Long parentId;
}
