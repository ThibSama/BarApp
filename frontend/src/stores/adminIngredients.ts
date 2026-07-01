import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { fetchIngredients } from '@/services/catalogAdminApi';
import type { IngredientResponse } from '@/types/api';
import { describeAdminError } from '@/utils/adminErrors';

/**
 * Read-mostly ingredient store used to power the cocktail form's autocomplete.
 * The cocktail API resolves/creates ingredients by name, so the form only needs
 * the existing ingredient list for suggestions and duplicate avoidance.
 */
export const useAdminIngredientsStore = defineStore('adminIngredients', () => {
  const items = ref<IngredientResponse[]>([]);
  const loading = ref(false);
  const error = ref('');
  const loaded = ref(false);

  let inFlight = false;
  let listSeq = 0;

  const activeItems = computed(() => items.value.filter((ingredient) => ingredient.active));

  async function load(options: { initial?: boolean } = {}): Promise<void> {
    if (inFlight) return;
    inFlight = true;
    const seq = ++listSeq;
    if (options.initial && !loaded.value) loading.value = true;
    try {
      const data = await fetchIngredients();
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

  return { items, activeItems, loading, error, loaded, load };
});
