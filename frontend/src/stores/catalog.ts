import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import { loadCategories, loadCocktails, saveCategories, saveCocktails } from '@/services/catalogService';
import type { Category, Cocktail, CocktailFormData } from '@/types/domain';

function slugify(value: string): string {
  return value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
}

export const useCatalogStore = defineStore('catalog', () => {
  const categories = ref<Category[]>(loadCategories());
  const cocktails = ref<Cocktail[]>(loadCocktails());

  watch(categories, (value) => saveCategories(value), { deep: true });
  watch(cocktails, (value) => saveCocktails(value), { deep: true });

  const enabledCategories = computed(() => categories.value.filter((category) => category.enabled));
  const enabledCocktails = computed(() => cocktails.value.filter((cocktail) => cocktail.available && enabledCategories.value.some((category) => category.id === cocktail.categoryId)));

  function getCategoryById(id: string): Category | undefined {
    return categories.value.find((category) => category.id === id);
  }

  function getCocktailById(id: string): Cocktail | undefined {
    return cocktails.value.find((cocktail) => cocktail.id === id);
  }

  function createCategory(name: string, description: string): void {
    categories.value.push({ id: `${slugify(name)}-${Date.now()}`, name: name.trim(), description: description.trim(), enabled: true });
  }

  function updateCategory(id: string, payload: Pick<Category, 'name' | 'description'>): void {
    const category = getCategoryById(id);
    if (category) Object.assign(category, { name: payload.name.trim(), description: payload.description.trim() });
  }

  function toggleCategory(id: string): void {
    const category = getCategoryById(id);
    if (category) category.enabled = !category.enabled;
  }

  function deleteCategory(id: string): void {
    categories.value = categories.value.filter((category) => category.id !== id);
    cocktails.value = cocktails.value.map((cocktail) => cocktail.categoryId === id ? { ...cocktail, available: false } : cocktail);
  }

  function createCocktail(payload: CocktailFormData): string {
    const id = `${slugify(payload.name)}-${Date.now()}`;
    cocktails.value.push({ id, ...payload, ingredients: payload.ingredients.filter((ingredient) => ingredient.trim()).map((ingredient) => ingredient.trim()) });
    return id;
  }

  function updateCocktail(id: string, payload: CocktailFormData): void {
    const index = cocktails.value.findIndex((cocktail) => cocktail.id === id);
    if (index >= 0) cocktails.value[index] = { id, ...payload, ingredients: payload.ingredients.filter((ingredient) => ingredient.trim()).map((ingredient) => ingredient.trim()) };
  }

  function toggleCocktail(id: string): void {
    const cocktail = getCocktailById(id);
    if (cocktail) cocktail.available = !cocktail.available;
  }

  function deleteCocktail(id: string): void {
    cocktails.value = cocktails.value.filter((cocktail) => cocktail.id !== id);
  }

  return { categories, cocktails, enabledCategories, enabledCocktails, getCategoryById, getCocktailById, createCategory, updateCategory, toggleCategory, deleteCategory, createCocktail, updateCocktail, toggleCocktail, deleteCocktail };
});
