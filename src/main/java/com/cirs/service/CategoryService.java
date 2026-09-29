package com.cirs.service;

import com.cirs.dto.request.CategoryRequest;
import com.cirs.dto.response.CategoryResponse;
import com.cirs.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getActiveCategories();
    List<CategoryResponse> getAllCategories();
    CategoryResponse getCategoryById(Long id);
    CategoryResponse createCategory(CategoryRequest request, User admin, HttpServletRequest httpRequest);
    CategoryResponse updateCategory(Long id, CategoryRequest request, User admin, HttpServletRequest httpRequest);
    CategoryResponse toggleCategoryStatus(Long id, User admin, HttpServletRequest httpRequest);
}
