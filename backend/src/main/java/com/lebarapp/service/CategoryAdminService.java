package com.lebarapp.service;

import com.lebarapp.dto.CategoryRequest;
import com.lebarapp.dto.CategoryResponse;
import com.lebarapp.entity.Category;
import com.lebarapp.exception.CategoryAlreadyExistsException;
import com.lebarapp.exception.CategoryNotFoundException;
import com.lebarapp.mapper.CategoryMapper;
import com.lebarapp.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Protected catalogue management for categories (CRUD with logical deletion).
 * Reads are read-only transactions; writes run transactionally and enforce
 * case-insensitive name uniqueness. Entities never leak past this boundary.
 */
@Service
public class CategoryAdminService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryAdminService(CategoryRepository categoryRepository, CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /** Management listing including inactive categories, deterministically ordered. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAscIdAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new CategoryAlreadyExistsException(name);
        }
        Category category = Category.create(
                name,
                normalizeDescription(request.description()),
                request.displayOrder(),
                request.active() == null || request.active());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, categoryId)) {
            throw new CategoryAlreadyExistsException(name);
        }
        category.update(
                name,
                normalizeDescription(request.description()),
                request.displayOrder(),
                request.active() == null || request.active());
        return categoryMapper.toResponse(category);
    }

    /** Logical deletion: deactivate the category, preserving its cocktails/history. */
    @Transactional
    public void deactivate(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.deactivate();
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
