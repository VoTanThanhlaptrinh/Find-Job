package com.nlu.recruitment.api;

import com.nlu.recruitment.api.dto.category.CategoryResponse;
import com.nlu.recruitment.application.CategoryService;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1/categories", produces = "application/json")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"),
                categoryService.getAllCategories(),
                HttpStatus.OK.value()
        ));
    }
}
