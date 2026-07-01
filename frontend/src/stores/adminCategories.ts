import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import {
  createCategory,
  deleteCategory,
  fetchCategories,
  updateCategory,
} from '@/services/catalogAdminApi';
import type { CategoryRequest, CategoryResponse } from '@/types/api';
import { describeAdminError } from '@/utils/adminErrors';

/**
 * Barmaker category administration store. Loads active and inactive categories,
 * guards against duplicate concurrent loads and stale responses, tracks
 * per-entity pending state, and deterministically refreshes the list after each
 * mutation. The list is never mutated locally before the API confirms.
 */
export const useAdminCategoriesStore = defineStore('adminCategories', () => {
  const items = ref<CategoryResponse[]>([]);
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

  function sortInPlace(list: CategoryResponse[]): CategoryResponse[] {
    return [...list].sort((a, b) => a.displayOrder - b.displayOrder || a.id - b.id);
  }

  async function load(options: { initial?: boolean } = {}): Promise<void> {
    if (inFlight) return;
    inFlight = true;
    const seq = ++listSeq;
    if (options.initial && !loaded.value) loading.value = true;
    try {
      const data = await fetchCategories();
      if (seq !== listSeq) return;
      items.value = sortInPlace(data);
      loaded.value = true;
      error.value = '';
    } catch (err) {
      if (seq === listSeq) error.value = describeAdminError(err);
    } finally {
      inFlight = false;
      if (seq === listSeq) loading.value = false;
    }
  }

  async function withPending<T>(id: number, action: () => Promise<T>): Promise<T> {
    pendingIds.value = [...pendingIds.value, id];
    try {
      return await action();
    } finally {
      pendingIds.value = pendingIds.value.filter((entry) => entry !== id);
    }
  }

  /** Create a category; on success the list is refreshed. Throws on failure. */
  async function create(body: CategoryRequest): Promise<CategoryResponse> {
    const created = await createCategory(body);
    await load();
    return created;
  }

  async function update(id: number, body: CategoryRequest): Promise<CategoryResponse> {
    const updated = await updateCategory(id, body);
    await load();
    return updated;
  }

  /** Logical deactivation through DELETE; refreshes so the row shows inactive. */
  async function deactivate(id: number): Promise<void> {
    await withPending(id, async () => {
      await deleteCategory(id);
      await load();
    });
  }

  /** Reactivation via the supported PUT contract (active=true). */
  async function reactivate(category: CategoryResponse): Promise<void> {
    await withPending(category.id, async () => {
      await updateCategory(category.id, {
        name: category.name,
        description: category.description,
        displayOrder: category.displayOrder,
        active: true,
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
    create,
    update,
    deactivate,
    reactivate,
  };
});
