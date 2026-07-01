import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { ApiError } from '@/services/apiClient';
import { fetchMenu } from '@/services/menuApi';
import type { MenuCategory, MenuCocktail } from '@/types/api';

/** A menu cocktail flattened with its owning category, for filtering/lookup. */
export interface MenuCocktailView extends MenuCocktail {
  categoryId: number;
  categoryName: string;
}

function describeError(err: unknown): string {
  if (err instanceof ApiError && err.isNetworkError) {
    return 'Le serveur est injoignable. Vérifiez votre connexion.';
  }
  return 'La carte n’a pas pu être chargée. Réessayez.';
}

/**
 * Public customer menu store backed by `GET /api/menu`. It deduplicates
 * concurrent loads, distinguishes the initial load from a background refresh,
 * and never falls back to fake data: on failure it keeps the last successful
 * menu and surfaces an error for the retry UI.
 */
export const useMenuStore = defineStore('menu', () => {
  const categories = ref<MenuCategory[]>([]);
  const selectedCategoryId = ref<number | 'all'>('all');
  const loading = ref(false);
  const refreshing = ref(false);
  const error = ref('');
  const loaded = ref(false);
  const lastLoadedAt = ref<number | null>(null);

  // Shared in-flight promise so simultaneous callers trigger a single request.
  let inFlight: Promise<void> | null = null;

  const cocktails = computed<MenuCocktailView[]>(() =>
    categories.value.flatMap((category) =>
      category.cocktails.map((cocktail) => ({
        ...cocktail,
        categoryId: category.id,
        categoryName: category.name,
      })),
    ),
  );

  function getCocktailById(id: number): MenuCocktailView | undefined {
    return cocktails.value.find((cocktail) => cocktail.id === id);
  }

  function load(options: { initial?: boolean } = {}): Promise<void> {
    if (inFlight) return inFlight;
    if (options.initial && !loaded.value) loading.value = true;
    else refreshing.value = true;

    inFlight = (async () => {
      try {
        const response = await fetchMenu();
        // Deterministic ordering: backend already orders, but enforce it locally
        // so the UI never depends on serialization order.
        categories.value = [...response.categories].sort(
          (a, b) => a.displayOrder - b.displayOrder || a.id - b.id,
        );
        loaded.value = true;
        error.value = '';
        lastLoadedAt.value = Date.now();
      } catch (err) {
        // Retain the last successful menu; only surface a controlled error.
        error.value = describeError(err);
      } finally {
        loading.value = false;
        refreshing.value = false;
        inFlight = null;
      }
    })();
    return inFlight;
  }

  function retry(): Promise<void> {
    return load({ initial: !loaded.value });
  }

  return {
    categories,
    cocktails,
    selectedCategoryId,
    loading,
    refreshing,
    error,
    loaded,
    lastLoadedAt,
    getCocktailById,
    load,
    retry,
  };
});
