package com.lebarapp.controller;

import com.lebarapp.dto.CategoryRequest;
import com.lebarapp.dto.CategoryResponse;
import com.lebarapp.service.CategoryAdminService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Protected barmaker category-management API. Every route lives under
 * {@code /api/bar/**} and therefore requires {@code ROLE_BARMAKER} (enforced by
 * the security filter chain). {@code DELETE} performs a logical deactivation,
 * never a physical delete. Entities are never exposed; only DTOs cross this
 * boundary and validation/error mapping is centralized.
 */
@RestController
@RequestMapping("/api/bar/categories")
public class BarCategoryController {

    private final CategoryAdminService categoryService;

    public BarCategoryController(CategoryAdminService categoryService) {
        this.categoryService = categoryService;
    }

    /** Lists every category (active and inactive) for management. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CategoryResponse> list() {
        return categoryService.list();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request,
                                                   UriComponentsBuilder uriBuilder) {
        CategoryResponse created = categoryService.create(request);
        URI location = uriBuilder.path("/api/bar/categories/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(value = "/{categoryId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CategoryResponse update(@PathVariable Long categoryId,
                                   @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(categoryId, request);
    }

    /** Logical deletion (désactivation): 204 No Content. */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long categoryId) {
        categoryService.deactivate(categoryId);
        return ResponseEntity.noContent().build();
    }
}
