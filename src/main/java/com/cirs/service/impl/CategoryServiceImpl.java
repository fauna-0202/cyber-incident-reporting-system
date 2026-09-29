package com.cirs.service.impl;

import com.cirs.dto.request.CategoryRequest;
import com.cirs.dto.response.CategoryResponse;
import com.cirs.entity.IncidentCategory;
import com.cirs.entity.User;
import com.cirs.exception.BadRequestException;
import com.cirs.exception.ResourceNotFoundException;
import com.cirs.repository.IncidentCategoryRepository;
import com.cirs.service.AuditService;
import com.cirs.service.CategoryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final IncidentCategoryRepository categoryRepository;
    private final AuditService auditService;

    public CategoryServiceImpl(IncidentCategoryRepository categoryRepository, AuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        IncidentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident category not found with id: " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request, User admin, HttpServletRequest httpRequest) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Incident category with name '" + request.getName() + "' already exists");
        }

        IncidentCategory category = new IncidentCategory(request.getName(), request.getDescription());
        category.setActive(request.isActive());
        IncidentCategory saved = categoryRepository.save(category);

        auditService.log(admin, "CREATE_CATEGORY", "CATEGORY", saved.getId().toString(),
                "Created incident category: " + saved.getName(), httpRequest);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request, User admin, HttpServletRequest httpRequest) {
        IncidentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(request.getName()) &&
                categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setActive(request.isActive());
        IncidentCategory updated = categoryRepository.save(category);

        auditService.log(admin, "UPDATE_CATEGORY", "CATEGORY", updated.getId().toString(),
                "Updated incident category: " + updated.getName(), httpRequest);

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public CategoryResponse toggleCategoryStatus(Long id, User admin, HttpServletRequest httpRequest) {
        IncidentCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident category not found with id: " + id));

        category.setActive(!category.isActive());
        IncidentCategory updated = categoryRepository.save(category);

        auditService.log(admin, "TOGGLE_CATEGORY_STATUS", "CATEGORY", updated.getId().toString(),
                "Category " + updated.getName() + " active status set to: " + updated.isActive(), httpRequest);

        return mapToResponse(updated);
    }

    private CategoryResponse mapToResponse(IncidentCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt()
        );
    }
}
