package com.lebarapp.service;

import com.lebarapp.dto.CocktailIngredientRequest;
import com.lebarapp.dto.CocktailPriceRequest;
import com.lebarapp.dto.CocktailRequest;
import com.lebarapp.dto.CocktailResponse;
import com.lebarapp.entity.Category;
import com.lebarapp.entity.Cocktail;
import com.lebarapp.entity.CocktailIngredient;
import com.lebarapp.entity.CocktailPrice;
import com.lebarapp.entity.Ingredient;
import com.lebarapp.enums.CocktailSize;
import com.lebarapp.exception.CategoryNotFoundException;
import com.lebarapp.exception.CocktailAlreadyExistsException;
import com.lebarapp.exception.CocktailNotFoundException;
import com.lebarapp.exception.InactiveCategoryException;
import com.lebarapp.exception.InvalidCatalogRequestException;
import com.lebarapp.mapper.CocktailAdminMapper;
import com.lebarapp.repository.CategoryRepository;
import com.lebarapp.repository.CocktailIngredientRepository;
import com.lebarapp.repository.CocktailPriceRepository;
import com.lebarapp.repository.CocktailRepository;
import com.lebarapp.repository.IngredientRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Protected catalogue management for cocktails (CRUD with logical deletion).
 *
 * <p>The cocktail is treated as an aggregate: its scalar fields plus the
 * ingredient and price children are managed transactionally through explicit
 * child repositories (strategy B — delete/insert and upsert replacement) rather
 * than JPA cascades, so a single write atomically (re)builds a deterministic set
 * of rows. Ingredients are reused case-insensitively across cocktails. Prices
 * are upserted in place, keeping exactly one active line per size S, M and L
 * under the {@code (cocktail_id, size)} unique constraint.</p>
 */
@Service
public class CocktailAdminService {

    private static final Comparator<Cocktail> MANAGEMENT_ORDER =
            Comparator.comparingInt((Cocktail c) -> c.getCategory().getDisplayOrder())
                    .thenComparing(Cocktail::getName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Cocktail::getId);

    private final CocktailRepository cocktailRepository;
    private final CategoryRepository categoryRepository;
    private final IngredientRepository ingredientRepository;
    private final CocktailIngredientRepository cocktailIngredientRepository;
    private final CocktailPriceRepository cocktailPriceRepository;
    private final CocktailAdminMapper cocktailMapper;

    @PersistenceContext
    private EntityManager entityManager;

    public CocktailAdminService(CocktailRepository cocktailRepository,
                                CategoryRepository categoryRepository,
                                IngredientRepository ingredientRepository,
                                CocktailIngredientRepository cocktailIngredientRepository,
                                CocktailPriceRepository cocktailPriceRepository,
                                CocktailAdminMapper cocktailMapper) {
        this.cocktailRepository = cocktailRepository;
        this.categoryRepository = categoryRepository;
        this.ingredientRepository = ingredientRepository;
        this.cocktailIngredientRepository = cocktailIngredientRepository;
        this.cocktailPriceRepository = cocktailPriceRepository;
        this.cocktailMapper = cocktailMapper;
    }

    /** Management listing including inactive cocktails, deterministically ordered. */
    @Transactional(readOnly = true)
    public List<CocktailResponse> list() {
        return cocktailRepository.findAllForManagement().stream()
                .sorted(MANAGEMENT_ORDER)
                .map(cocktailMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CocktailResponse getById(Long cocktailId) {
        Cocktail cocktail = cocktailRepository.findWithDetailById(cocktailId)
                .orElseThrow(() -> new CocktailNotFoundException(cocktailId));
        return cocktailMapper.toResponse(cocktail);
    }

    @Transactional
    public CocktailResponse create(CocktailRequest request) {
        validateSemantics(request);
        Category category = resolveActiveCategory(request.categoryId());
        String name = request.name().trim();
        if (cocktailRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), name)) {
            throw new CocktailAlreadyExistsException(name);
        }

        Cocktail cocktail = Cocktail.create(
                category,
                name,
                request.description().trim(),
                normalize(request.shortDescription()),
                normalize(request.imageUrl()),
                request.active() == null || request.active());
        cocktailRepository.save(cocktail);

        List<CocktailIngredient> ingredients = replaceIngredients(cocktail, request);
        List<CocktailPrice> prices = replacePrices(cocktail, request);
        cocktailRepository.flush();
        return cocktailMapper.toResponse(cocktail, ingredients, prices);
    }

    @Transactional
    public CocktailResponse update(Long cocktailId, CocktailRequest request) {
        validateSemantics(request);
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new CocktailNotFoundException(cocktailId));
        Category category = resolveActiveCategory(request.categoryId());
        String name = request.name().trim();
        if (cocktailRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(category.getId(), name, cocktailId)) {
            throw new CocktailAlreadyExistsException(name);
        }

        cocktail.updateDetails(
                name,
                request.description().trim(),
                normalize(request.shortDescription()),
                normalize(request.imageUrl()),
                request.active() == null || request.active());
        cocktail.changeCategory(category);

        List<CocktailIngredient> ingredients = replaceIngredients(cocktail, request);
        List<CocktailPrice> prices = replacePrices(cocktail, request);
        cocktailRepository.flush();
        return cocktailMapper.toResponse(cocktail, ingredients, prices);
    }

    /** Logical deletion: deactivate the cocktail, preserving order snapshots. */
    @Transactional
    public void deactivate(Long cocktailId) {
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new CocktailNotFoundException(cocktailId));
        cocktail.deactivate();
    }

    // --- internals ---------------------------------------------------------

    private Category resolveActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        if (!category.isActive()) {
            throw new InactiveCategoryException(categoryId);
        }
        return category;
    }

    /**
     * Replaces all ingredient associations with a fresh, deterministic set:
     * existing rows are bulk-deleted and flushed first, then the requested rows
     * are inserted (reusing ingredients case-insensitively). On create the delete
     * is a no-op. Runs inside the caller's transaction.
     */
    private List<CocktailIngredient> replaceIngredients(Cocktail cocktail, CocktailRequest request) {
        // Delete the existing (managed) associations and flush, so the rows are
        // gone from both the context and the database before the fresh set is
        // inserted — a reused ingredient can therefore re-take the same key.
        cocktailIngredientRepository.deleteAll(
                cocktailIngredientRepository.findByIdCocktailId(cocktail.getId()));
        cocktailIngredientRepository.flush();

        List<CocktailIngredient> created = new ArrayList<>(request.ingredients().size());
        for (CocktailIngredientRequest line : request.ingredients()) {
            Ingredient ingredient = resolveIngredient(line.name().trim());
            CocktailIngredient association = CocktailIngredient.create(
                    cocktail, ingredient, line.displayOrder(), normalize(line.quantityLabel()));
            // persist (not save): the row carries an assigned composite id, which
            // would otherwise send save(...) down the merge path.
            entityManager.persist(association);
            created.add(association);
        }
        return created;
    }

    /** Reuses an existing ingredient case-insensitively (reactivating it if needed). */
    private Ingredient resolveIngredient(String name) {
        return ingredientRepository.findByNameIgnoreCase(name)
                .map(existing -> {
                    if (!existing.isActive()) {
                        existing.activate();
                    }
                    return existing;
                })
                .orElseGet(() -> ingredientRepository.save(Ingredient.create(name)));
    }

    /**
     * Upserts exactly one active price per requested size in place. Existing rows
     * (at most one per size, by the unique constraint) are updated/reactivated;
     * missing sizes are inserted.
     */
    private List<CocktailPrice> replacePrices(Cocktail cocktail, CocktailRequest request) {
        Map<CocktailSize, CocktailPrice> existing = cocktailPriceRepository.findByCocktailId(cocktail.getId())
                .stream().collect(Collectors.toMap(CocktailPrice::getSize, Function.identity()));

        List<CocktailPrice> result = new ArrayList<>(request.prices().size());
        for (CocktailPriceRequest line : request.prices()) {
            CocktailPrice price = existing.get(line.size());
            if (price != null) {
                price.update(line.price());
                result.add(price);
            } else {
                result.add(cocktailPriceRepository.save(
                        CocktailPrice.create(cocktail, line.size(), line.price())));
            }
        }
        return result;
    }

    /**
     * Cross-line rules bean validation cannot express: ingredient names unique
     * (case-insensitive) inside the request, and exactly one price for each of
     * S, M and L.
     */
    private void validateSemantics(CocktailRequest request) {
        Set<String> ingredientNames = new HashSet<>();
        for (CocktailIngredientRequest line : request.ingredients()) {
            String key = line.name().trim().toLowerCase(Locale.ROOT);
            if (!ingredientNames.add(key)) {
                throw new InvalidCatalogRequestException(
                        "Les ingrédients doivent être uniques : « " + line.name().trim() + " » est en double.");
            }
        }

        Set<CocktailSize> sizes = EnumSet.noneOf(CocktailSize.class);
        for (CocktailPriceRequest line : request.prices()) {
            if (!sizes.add(line.size())) {
                throw new InvalidCatalogRequestException(
                        "La taille " + line.size() + " est fournie plusieurs fois.");
            }
        }
        if (!sizes.equals(EnumSet.allOf(CocktailSize.class))) {
            throw new InvalidCatalogRequestException(
                    "Les trois tailles S, M et L sont obligatoires (une et une seule fois chacune).");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
