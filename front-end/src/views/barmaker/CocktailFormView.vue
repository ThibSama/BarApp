<script setup lang="ts">
import { computed, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useCatalogStore } from '@/stores/catalog';
import type { CocktailFormData } from '@/types/domain';
import { validateCocktailForm } from '@/utils/validation';

const route = useRoute();
const router = useRouter();
const catalog = useCatalogStore();
const cocktailId = computed(() => String(route.params.cocktailId ?? ''));
const existing = computed(() => cocktailId.value ? catalog.getCocktailById(cocktailId.value) : undefined);
const form = reactive<CocktailFormData>({
  name: existing.value?.name ?? '',
  description: existing.value?.description ?? '',
  shortDescription: existing.value?.shortDescription ?? '',
  categoryId: existing.value?.categoryId ?? catalog.categories[0]?.id ?? '',
  imageUrl: existing.value?.imageUrl ?? 'https://images.unsplash.com/photo-1544145945-f90425340c7e?auto=format&fit=crop&w=900&q=80',
  ingredients: existing.value?.ingredients ? [...existing.value.ingredients] : [''],
  prices: { S: existing.value?.prices.S ?? 5, M: existing.value?.prices.M ?? 7, L: existing.value?.prices.L ?? 9 },
  available: existing.value?.available ?? true,
});
const validation = reactive({ errors: {} as Record<string, string> });
function addIngredient(): void { form.ingredients.push(''); }
function removeIngredient(index: number): void { form.ingredients.splice(index, 1); if (form.ingredients.length === 0) form.ingredients.push(''); }
function save(): void {
  const result = validateCocktailForm(form);
  validation.errors = result.errors;
  if (!result.valid) return;
  if (existing.value) catalog.updateCocktail(existing.value.id, form);
  else catalog.createCocktail(form);
  router.push('/barmaker/cocktails');
}
</script>

<template>
  <section class="stack">
    <RouterLink to="/barmaker/cocktails">← Retour aux cocktails</RouterLink>
    <div class="page-title"><div><p class="eyebrow">Gestion</p><h1>{{ existing ? 'Modifier un cocktail' : 'Créer un cocktail' }}</h1></div></div>
    <form class="card form-grid" @submit.prevent="save">
      <label>Nom<input v-model="form.name" type="text" /><span v-if="validation.errors.name" class="field-error">{{ validation.errors.name }}</span></label>
      <label>Description courte<input v-model="form.shortDescription" type="text" /><span v-if="validation.errors.shortDescription" class="field-error">{{ validation.errors.shortDescription }}</span></label>
      <label>Description complète<textarea v-model="form.description" rows="4"></textarea><span v-if="validation.errors.description" class="field-error">{{ validation.errors.description }}</span></label>
      <label>Catégorie<select v-model="form.categoryId"><option v-for="category in catalog.categories" :key="category.id" :value="category.id">{{ category.name }}</option></select><span v-if="validation.errors.categoryId" class="field-error">{{ validation.errors.categoryId }}</span></label>
      <label>Image<input v-model="form.imageUrl" type="url" /><span v-if="validation.errors.imageUrl" class="field-error">{{ validation.errors.imageUrl }}</span></label>
      <fieldset><legend>Ingrédients</legend><div v-for="(_, index) in form.ingredients" :key="index" class="ingredient-row"><input v-model="form.ingredients[index]" type="text" :aria-label="`Ingrédient ${index + 1}`" /><button class="button ghost" type="button" @click="removeIngredient(index)">Retirer</button></div><button class="button secondary" type="button" @click="addIngredient">Ajouter un ingrédient</button><span v-if="validation.errors.ingredients" class="field-error">{{ validation.errors.ingredients }}</span></fieldset>
      <div class="price-grid"><label>Prix S<input v-model.number="form.prices.S" type="number" min="0" step="0.5" /><span v-if="validation.errors.priceS" class="field-error">{{ validation.errors.priceS }}</span></label><label>Prix M<input v-model.number="form.prices.M" type="number" min="0" step="0.5" /><span v-if="validation.errors.priceM" class="field-error">{{ validation.errors.priceM }}</span></label><label>Prix L<input v-model.number="form.prices.L" type="number" min="0" step="0.5" /><span v-if="validation.errors.priceL" class="field-error">{{ validation.errors.priceL }}</span></label></div>
      <label class="checkbox"><input v-model="form.available" type="checkbox" /> Cocktail disponible</label>
      <div class="form-actions"><button class="button" type="submit">Enregistrer</button><button class="button secondary" type="button" @click="router.push('/barmaker/cocktails')">Annuler</button></div>
    </form>
  </section>
</template>

<style scoped>
.ingredient-row, .price-grid { display: grid; gap: 0.75rem; }
.ingredient-row { grid-template-columns: 1fr auto; margin-bottom: 0.5rem; }
.price-grid { grid-template-columns: repeat(auto-fit, minmax(10rem, 1fr)); }
.checkbox { display: flex; flex-direction: row; align-items: center; gap: 0.5rem; }
</style>
