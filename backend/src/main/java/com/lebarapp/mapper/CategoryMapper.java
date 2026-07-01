package com.lebarapp.mapper;

import com.lebarapp.dto.CategoryResponse;
import com.lebarapp.entity.Category;
import org.springframework.stereotype.Component;

/**
 * Explicit mapping from the {@link Category} entity to its management DTO.
 * Entities never cross this boundary.
 */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getDisplayOrder(),
                category.isActive());
    }
}
