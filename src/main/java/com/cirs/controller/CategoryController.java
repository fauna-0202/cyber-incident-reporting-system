package com.cirs.controller;

import com.cirs.dto.request.CategoryRequest;
import com.cirs.dto.response.ApiResponse;
import com.cirs.dto.response.CategoryResponse;
import com.cirs.entity.User;
import com.cirs.service.AuthService;
import com.cirs.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AuthService authService;

    public CategoryController(CategoryService categoryService, AuthService authService) {
        this.categoryService = categoryService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        List<CategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request,
                                                                       Authentication authentication,
                                                                       HttpServletRequest httpRequest) {
        User admin = authService.getUserByUsername(authentication.getName());
        CategoryResponse category = categoryService.createCategory(request, admin, httpRequest);
        return new ResponseEntity<>(ApiResponse.success("Category created successfully", category), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable Long id,
                                                                       @Valid @RequestBody CategoryRequest request,
                                                                       Authentication authentication,
                                                                       HttpServletRequest httpRequest) {
        User admin = authService.getUserByUsername(authentication.getName());
        CategoryResponse category = categoryService.updateCategory(id, request, admin, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleCategoryStatus(@PathVariable Long id,
                                                                             Authentication authentication,
                                                                             HttpServletRequest httpRequest) {
        User admin = authService.getUserByUsername(authentication.getName());
        CategoryResponse category = categoryService.toggleCategoryStatus(id, admin, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Category status updated", category));
    }
}
