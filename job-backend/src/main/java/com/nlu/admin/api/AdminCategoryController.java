package com.nlu.admin.api;

import com.nlu.recruitment.api.dto.category.CategoryRequest;
import com.nlu.recruitment.api.dto.category.CategoryResponse;
import com.nlu.recruitment.application.CategoryService;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/api/admin/categories", produces = "application/json")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                MessageUtils.getMessage("message.success"),
                response,
                HttpStatus.CREATED.value()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"),
                response,
                HttpStatus.OK.value()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"),
                Map.of("id", String.valueOf(id), "deleted", true),
                HttpStatus.OK.value()
        ));
    }
}
