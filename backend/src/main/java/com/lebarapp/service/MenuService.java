package com.lebarapp.service;

import com.lebarapp.dto.MenuCategoryDto;
import com.lebarapp.dto.MenuResponse;
import com.lebarapp.entity.Category;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.mapper.MenuMapper;
import com.lebarapp.repository.CategoryRepository;
import com.lebarapp.repository.CocktailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single read-only service boundary for the public menu. Builds the menu with
 * two bounded queries (active categories, then active cocktails with their
 * associations eagerly fetched) to avoid N+1 access.
 */
@Service
public class MenuService {

    private final CategoryRepository categoryRepository;
    private final CocktailRepository cocktailRepository;
    private final MenuMapper menuMapper;

    public MenuService(CategoryRepository categoryRepository,
                       CocktailRepository cocktailRepository,
                       MenuMapper menuMapper) {
        this.categoryRepository = categoryRepository;
        this.cocktailRepository = cocktailRepository;
        this.menuMapper = menuMapper;
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenu() {
        List<Category> categories = categoryRepository.findByActiveTrueOrderByDisplayOrderAscNameAsc();

        Map<Long, List<Cocktail>> cocktailsByCategory = cocktailRepository.findActiveForMenu().stream()
                .collect(Collectors.groupingBy(c -> c.getCategory().getId()));

        List<MenuCategoryDto> categoryDtos = categories.stream()
                .map(category -> menuMapper.toCategoryDto(
                        category,
                        cocktailsByCategory.getOrDefault(category.getId(), List.of())))
                .toList();

        return new MenuResponse(categoryDtos);
    }
}
