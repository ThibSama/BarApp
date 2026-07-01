package com.lebarapp.controller;

import com.lebarapp.dto.IngredientRequest;
import com.lebarapp.dto.IngredientResponse;
import com.lebarapp.service.IngredientAdminService;
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
 * Protected barmaker ingredient-management API. Every route lives under
 * {@code /api/bar/**} and therefore requires an authenticated staff member
 * ({@code ROLE_BARMAKER} or {@code ROLE_MANAGER}, enforced by
 * the security filter chain). {@code DELETE} performs a logical deactivation,
 * never a physical delete, so cocktail associations and history are preserved.
 * Entities are never exposed; only DTOs cross this boundary.
 */
@RestController
@RequestMapping("/api/bar/ingredients")
public class BarIngredientController {

    private final IngredientAdminService ingredientService;

    public BarIngredientController(IngredientAdminService ingredientService) {
        this.ingredientService = ingredientService;
    }

    /** Lists every ingredient (active and inactive) for management. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IngredientResponse> list() {
        return ingredientService.list();
    }

    /** Full management detail of one ingredient. 404 if unknown. */
    @GetMapping(value = "/{ingredientId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public IngredientResponse getOne(@PathVariable Long ingredientId) {
        return ingredientService.getById(ingredientId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IngredientResponse> create(@Valid @RequestBody IngredientRequest request,
                                                     UriComponentsBuilder uriBuilder) {
        IngredientResponse created = ingredientService.create(request);
        URI location = uriBuilder.path("/api/bar/ingredients/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(value = "/{ingredientId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public IngredientResponse update(@PathVariable Long ingredientId,
                                     @Valid @RequestBody IngredientRequest request) {
        return ingredientService.update(ingredientId, request);
    }

    /** Logical deletion (désactivation): 204 No Content. */
    @DeleteMapping("/{ingredientId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long ingredientId) {
        ingredientService.deactivate(ingredientId);
        return ResponseEntity.noContent().build();
    }
}
