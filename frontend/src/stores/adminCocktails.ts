import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  createCocktail,
  deleteCocktail,
  fetchCocktail,
  fetchCocktails,
  updateCocktail,
} from '@/services/catalogAdminApi';
import type { CocktailRequest, CocktailResponse } from '@/types/api';
import { describeAdminError } from '@/utils/adminErrors';

/**
 * Barmaker cocktail administration store. Mirrors the category store: dedup'd
 * loads, stale-response protection, per-entity pending state, deterministic
 * refresh after each mutation, and logical deactivation via DELETE.
 */
export const useAdminCocktailsStore = defineStore('adminCocktails', () => {
  const items = ref<CocktailResponse[]>([]);
  const loading = ref(false);
  const error = ref('');
  const loaded = ref(false);
  const pendingIds = ref<number[]>([]);

  let inFlight = false;
  let listSeq = 0;

  const hasItems = computed(() => items.value.length > 0);

  function isPending(id: number): boolean {
    return pendingIds.value.includes(id);
  }

  async function load(options: { initial?: boolean } = {}): Promise<void> {
    if (inFlight) return;
    inFlight = true;
    const seq = ++listSeq;
    if (options.initial && !loaded.value) loading.value = true;
    try {
      const data = await fetchCocktails();
      if (seq !== listSeq) return;
      items.value = [...data].sort((a, b) => a.name.localeCompare(b.name, 'fr'));
      loaded.value = true;
      error.value = '';
    } catch (err) {
      if (seq === listSeq) error.value = describeAdminError(err);
    } finally {
      inFlight = false;
      if (seq === listSeq) loading.value = false;
    }
  }

  /** Load a single cocktail fresh for the edit modal (full ingredient/price set). */
  function loadOne(id: number): Promise<CocktailResponse> {
    return fetchCocktail(id);
  }

  async function withPending<T>(id: number, action: () => Promise<T>): Promise<T> {
    pendingIds.value = [...pendingIds.value, id];
    try {
      return await action();
    } finally {
      pendingIds.value = pendingIds.value.filter((entry) => entry !== id);
    }
  }

  async function create(body: CocktailRequest): Promise<CocktailResponse> {
    const created = await createCocktail(body);
    await load();
    return created;
  }

  async function update(id: number, body: CocktailRequest): Promise<CocktailResponse> {
    const updated = await updateCocktail(id, body);
    await load();
    return updated;
  }

  async function deactivate(id: number): Promise<void> {
    await withPending(id, async () => {
      await deleteCocktail(id);
      await load();
    });
  }

  /** Reactivation via PUT with the full current payload and active=true. */
  async function reactivate(cocktail: CocktailResponse): Promise<void> {
    await withPending(cocktail.id, async () => {
      await updateCocktail(cocktail.id, {
        categoryId: cocktail.categoryId,
        name: cocktail.name,
        description: cocktail.description,
        shortDescription: cocktail.shortDescription,
        imageUrl: cocktail.imageUrl,
        active: true,
        ingredients: cocktail.ingredients.map((ingredient) => ({
          name: ingredient.name,
          quantityLabel: ingredient.quantityLabel,
          displayOrder: ingredient.displayOrder,
        })),
        prices: cocktail.prices.map((price) => ({ size: price.size, price: price.price })),
      });
      await load();
    });
  }

  return {
    items,
    loading,
    error,
    loaded,
    pendingIds,
    hasItems,
    isPending,
    load,
    loadOne,
    create,
    update,
    deactivate,
    reactivate,
    describeError: describeAdminError,
  };
});
