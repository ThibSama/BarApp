package com.lebarapp.controller;

import com.lebarapp.dto.CocktailRequest;
import com.lebarapp.dto.CocktailResponse;
import com.lebarapp.service.CocktailAdminService;
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
 * Protected barmaker cocktail-management API. Every route lives under
 * {@code /api/bar/**} and therefore requires an authenticated staff member
 * ({@code ROLE_BARMAKER} or {@code ROLE_MANAGER}). {@code DELETE}
 * performs a logical deactivation, preserving order-item snapshots. Entities are
 * never exposed; only DTOs cross this boundary.
 */
@RestController
@RequestMapping("/api/bar/cocktails")
public class BarCocktailController {

    private final CocktailAdminService cocktailService;

    public BarCocktailController(CocktailAdminService cocktailService) {
        this.cocktailService = cocktailService;
    }

    /** Lists every cocktail (active and inactive) for management. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CocktailResponse> list() {
        return cocktailService.list();
    }

    /** Full management detail of one cocktail. 404 if unknown. */
    @GetMapping(value = "/{cocktailId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public CocktailResponse getOne(@PathVariable Long cocktailId) {
        return cocktailService.getById(cocktailId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CocktailResponse> create(@Valid @RequestBody CocktailRequest request,
                                                    UriComponentsBuilder uriBuilder) {
        CocktailResponse created = cocktailService.create(request);
        URI location = uriBuilder.path("/api/bar/cocktails/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping(value = "/{cocktailId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CocktailResponse update(@PathVariable Long cocktailId,
                                   @Valid @RequestBody CocktailRequest request) {
        return cocktailService.update(cocktailId, request);
    }

    /** Logical deletion (désactivation): 204 No Content. */
    @DeleteMapping("/{cocktailId}")
    public ResponseEntity<Void> deactivate(@PathVariable Long cocktailId) {
        cocktailService.deactivate(cocktailId);
        return ResponseEntity.noContent().build();
    }
}
