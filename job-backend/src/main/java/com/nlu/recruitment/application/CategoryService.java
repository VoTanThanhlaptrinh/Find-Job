package com.nlu.recruitment.application;

import com.nlu.recruitment.api.dto.category.CategoryRequest;
import com.nlu.recruitment.api.dto.category.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}
