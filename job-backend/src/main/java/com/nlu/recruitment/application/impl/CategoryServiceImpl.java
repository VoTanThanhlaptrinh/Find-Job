package com.nlu.recruitment.application.impl;

import com.nlu.recruitment.api.dto.category.CategoryRequest;
import com.nlu.recruitment.api.dto.category.CategoryResponse;
import com.nlu.recruitment.application.CategoryService;
import com.nlu.recruitment.domain.model.Category;
import com.nlu.recruitment.domain.repository.CategoryRepository;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.model.EntityStatus;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findByRecordStatus(EntityStatus.ACTIVE).stream()
                .map(CategoryResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BadRequestException("validation.category.name.required");
        }

        Category category = new Category();
        category.setName(request.getName());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BadRequestException("validation.category.parent.notfound"));
            category.setParentCategory(parent);
        }

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("validation.category.notfound"));

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            category.setName(request.getName());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BadRequestException("validation.category.parent.self");
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BadRequestException("validation.category.parent.notfound"));
            category.setParentCategory(parent);
        } else {
            category.setParentCategory(null);
        }

        return CategoryResponse.fromEntity(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("validation.category.notfound"));

        // Check if category has any jobs attached
        if (category.getJobs() != null && !category.getJobs().isEmpty()) {
            throw new BadRequestException("validation.category.delete.hasjobs");
        }

        // Check if category has subcategories that have jobs (recursive check could be complex, simple check here)
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            throw new BadRequestException("validation.category.delete.hassubcategories");
        }

        category.markDeleted();
        categoryRepository.save(category);
    }
}
