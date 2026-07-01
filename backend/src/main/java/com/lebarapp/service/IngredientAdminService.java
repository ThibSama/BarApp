package com.lebarapp.service;

import com.lebarapp.dto.IngredientRequest;
import com.lebarapp.dto.IngredientResponse;
import com.lebarapp.entity.Ingredient;
import com.lebarapp.exception.IngredientAlreadyExistsException;
import com.lebarapp.exception.IngredientNotFoundException;
import com.lebarapp.mapper.IngredientMapper;
import com.lebarapp.repository.IngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Protected autonomous administration for ingredients (CRUD with logical
 * deletion). Reads are read-only transactions; writes run transactionally and
 * enforce case-insensitive name uniqueness through database-backed checks.
 *
 * <p>This service shares the {@link IngredientRepository#findByNameIgnoreCase}
 * lookup with the cocktail aggregate, so the case-insensitive resolution rule is
 * defined once. It does not duplicate the cocktail's reuse/reactivation
 * resolution: creating an ingredient here is an explicit operation that rejects
 * a case-insensitive duplicate rather than silently reusing it. Reactivation is
 * an explicit {@code PUT} (or happens automatically when the ingredient is
 * reused inside a cocktail, in {@code CocktailAdminService}).</p>
 */
@Service
public class IngredientAdminService {

    private final IngredientRepository ingredientRepository;
    private final IngredientMapper ingredientMapper;

    public IngredientAdminService(IngredientRepository ingredientRepository,
                                  IngredientMapper ingredientMapper) {
        this.ingredientRepository = ingredientRepository;
        this.ingredientMapper = ingredientMapper;
    }

    /** Management listing including inactive ingredients, deterministically ordered. */
    @Transactional(readOnly = true)
    public List<IngredientResponse> list() {
        return ingredientRepository.findAllByOrderByActiveDescNameAscIdAsc().stream()
                .map(ingredientMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IngredientResponse getById(Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));
        return ingredientMapper.toResponse(ingredient);
    }

    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        String name = request.name().trim();
        if (ingredientRepository.existsByNameIgnoreCase(name)) {
            throw new IngredientAlreadyExistsException(name);
        }
        Ingredient ingredient = Ingredient.create(name);
        if (request.active() != null && !request.active()) {
            ingredient.deactivate();
        }
        return ingredientMapper.toResponse(ingredientRepository.save(ingredient));
    }

    @Transactional
    public IngredientResponse update(Long ingredientId, IngredientRequest request) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));

        String name = request.name().trim();
        if (ingredientRepository.existsByNameIgnoreCaseAndIdNot(name, ingredientId)) {
            throw new IngredientAlreadyExistsException(name);
        }
        ingredient.update(name, request.active() == null || request.active());
        return ingredientMapper.toResponse(ingredient);
    }

    /**
     * Logical deletion: deactivates the ingredient, preserving the row and every
     * cocktail association/historical reference. Idempotent: deactivating an
     * already-inactive ingredient is a no-op that still returns successfully.
     */
    @Transactional
    public void deactivate(Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new IngredientNotFoundException(ingredientId));
        ingredient.deactivate();
    }
}
