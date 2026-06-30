<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import CocktailImage from '@/components/common/CocktailImage.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useCatalogStore } from '@/stores/catalog';
import type { CocktailFormData } from '@/types/domain';
import { formatCurrency } from '@/utils/formatters';
import { validateCocktailForm } from '@/utils/validation';

const props = withDefaults(defineProps<{
  cocktailId?: string;
  formId?: string;
  showFooter?: boolean;
  cancelLabel?: string;
}>(), {
  formId: 'cocktail-form',
  showFooter: true,
  cancelLabel: 'Annuler',
});

const emit = defineEmits<{ saved: []; cancel: []; 'submitting-change': [value: boolean] }>();
const catalog = useCatalogStore();
const submitting = ref(false);
const validation = reactive({ errors: {} as Record<string, string> });
const formError = ref('');
const existing = computed(() => props.cocktailId ? catalog.getCocktailById(props.cocktailId) : undefined);

function initialForm(): CocktailFormData {
  return {
    name: existing.value?.name ?? '',
    description: existing.value?.description ?? '',
    shortDescription: existing.value?.shortDescription ?? '',
    categoryId: existing.value?.categoryId ?? catalog.categories[0]?.id ?? '',
    imageUrl: existing.value?.imageUrl ?? '',
    ingredients: existing.value?.ingredients ? [...existing.value.ingredients] : [''],
    prices: { S: existing.value?.prices.S ?? 5, M: existing.value?.prices.M ?? 7, L: existing.value?.prices.L ?? 9 },
    available: existing.value?.available ?? true,
  };
}

const form = reactive<CocktailFormData>(initialForm());

function reset(): void {
  Object.assign(form, initialForm());
  form.ingredients = [...initialForm().ingredients];
  validation.errors = {};
  formError.value = '';
}

function addIngredient(): void { form.ingredients.push(''); }
function removeIngredient(index: number): void { form.ingredients.splice(index, 1); if (form.ingredients.length === 0) form.ingredients.push(''); }

async function save(): Promise<void> {
  formError.value = '';
  const result = validateCocktailForm(form);
  validation.errors = result.errors;
  if (!result.valid || submitting.value) return;
  submitting.value = true;
  emit('submitting-change', true);
  try {
    await Promise.resolve();
    const payload = { ...form, ingredients: form.ingredients.filter((ingredient) => ingredient.trim()) };
    if (existing.value) catalog.updateCocktail(existing.value.id, payload);
    else catalog.createCocktail(payload);
    reset();
    emit('saved');
  } catch {
    formError.value = 'Impossible d’enregistrer ce cocktail pour le moment.';
  } finally {
    submitting.value = false;
    emit('submitting-change', false);
  }
}

watch(() => props.cocktailId, reset);
defineExpose({ reset, save, submitting });
</script>

<template>
  <form :id="formId" class="admin-form" @submit.prevent="save">
    <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>
    <section class="form-section">
      <div class="section-heading"><h2>Informations générales</h2><p class="muted">Nom, descriptions et catégorie d’affichage.</p></div>
      <div class="form-grid">
        <label>Nom <input v-model="form.name" type="text" /><span v-if="validation.errors.name" class="field-error">{{ validation.errors.name }}</span></label>
        <label>Description courte <input v-model="form.shortDescription" type="text" /><span v-if="validation.errors.shortDescription" class="field-error">{{ validation.errors.shortDescription }}</span></label>
        <label class="wide">Description complète <textarea v-model="form.description" rows="4"></textarea><span v-if="validation.errors.description" class="field-error">{{ validation.errors.description }}</span></label>
        <label>Catégorie <select v-model="form.categoryId"><option v-for="category in catalog.categories" :key="category.id" :value="category.id">{{ category.name }}</option></select><span v-if="validation.errors.categoryId" class="field-error">{{ validation.errors.categoryId }}</span></label>
      </div>
    </section>

    <section class="form-section">
      <div class="section-heading"><h2>Image</h2><p class="muted">URL utilisée par la carte client et l’espace Barmaker.</p></div>
      <div class="image-grid">
        <label>URL de l’image <input v-model="form.imageUrl" type="url" /><span v-if="validation.errors.imageUrl" class="field-error">{{ validation.errors.imageUrl }}</span></label>
        <div class="image-preview"><CocktailImage :image-url="form.imageUrl" :cocktail-name="form.name || 'en cours de création'" /></div>
      </div>
    </section>

    <section class="form-section">
      <div class="section-heading"><h2>Ingrédients</h2><p class="muted">Les lignes vides sont ignorées à l’enregistrement.</p></div>
      <div class="ingredient-list"><div v-for="(_, index) in form.ingredients" :key="index" class="ingredient-row"><label>Ingrédient {{ index + 1 }}<input v-model="form.ingredients[index]" type="text" /></label><button class="icon-button danger" type="button" :aria-label="`Retirer l’ingrédient ${index + 1}`" @click="removeIngredient(index)"><AppIcon name="trash" :size="18" /></button></div></div>
      <button class="admin-action secondary add-ingredient" type="button" @click="addIngredient"><AppIcon name="plus" :size="17" />Ajouter un ingrédient</button>
      <span v-if="validation.errors.ingredients" class="field-error">{{ validation.errors.ingredients }}</span>
    </section>

    <section class="form-section">
      <div class="section-heading"><h2>Tailles et prix</h2><p class="muted">Tarifs affichés en euros pour chaque taille.</p></div>
      <div class="price-grid"><label v-for="size in ['S','M','L']" :key="size">Prix {{ size }} <span class="price-field"><input v-model.number="form.prices[size as 'S' | 'M' | 'L']" type="number" min="0" step="0.5" /><span>€</span></span><small>{{ formatCurrency(form.prices[size as 'S' | 'M' | 'L'] || 0) }}</small><span v-if="validation.errors[`price${size}`]" class="field-error">{{ validation.errors[`price${size}`] }}</span></label></div>
    </section>

    <section class="form-section availability-row">
      <div class="section-heading"><h2>Disponibilité</h2><p class="muted">Un cocktail indisponible disparaît de la carte client.</p></div>
      <label class="switch"><input v-model="form.available" type="checkbox" /><span></span>Cocktail disponible</label>
    </section>

    <div v-if="showFooter" class="form-footer"><button class="button" type="submit" :disabled="submitting"><AppIcon name="check" :size="18" />{{ submitting ? 'Enregistrement…' : 'Enregistrer' }}</button><button class="button secondary" type="button" :disabled="submitting" @click="emit('cancel')">{{ cancelLabel }}</button></div>
  </form>
</template>

<style scoped>
.admin-form { display: grid; gap: var(--space-4); }
.form-section { display: grid; gap: var(--space-4); padding: var(--space-5); border: 1px solid rgba(229,219,204,0.86); border-radius: 18px; background: rgba(255,255,255,0.88); box-shadow: 0 1px 2px rgba(29, 43, 31, 0.03); }
.section-heading { display: grid; gap: 4px; }
.section-heading h2 { margin: 0; font-size: 1.08rem; letter-spacing: -0.025em; }
.section-heading p { margin: 0; line-height: 1.45; }
.form-grid, .image-grid, .price-grid { display: grid; gap: var(--space-4); }
.form-section input, .form-section select, .form-section textarea { min-height: 50px; border-color: var(--color-border); border-radius: 14px; }
.wide { grid-column: 1 / -1; }
.image-preview { min-height: 136px; overflow: hidden; border: 1px dashed var(--color-border-strong); border-radius: 18px; background: #f4efe6; }
.image-preview img { width: 100%; height: 100%; min-height: 136px; object-fit: cover; }
.ingredient-list { display: grid; gap: var(--space-3); }
.ingredient-row { display: grid; grid-template-columns: minmax(0, 1fr) 44px; gap: var(--space-3); align-items: end; }
.icon-button { width: 44px; height: 44px; display: inline-grid; place-items: center; border: 0; border-radius: var(--radius-round); background: #f8f6f2; color: var(--color-primary); cursor: pointer; }
.icon-button.danger:hover { background: #fde8e7; color: var(--color-error); }
.admin-action { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-2); min-height: 44px; padding: 0 14px; border-radius: var(--radius-medium); font-weight: 800; cursor: pointer; }
.admin-action.secondary { border: 1px solid transparent; background: #f8f6f2; color: var(--color-primary); }
.add-ingredient { justify-self: start; }
.price-field { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: center; border: 1px solid var(--color-border); border-radius: var(--radius-medium); background: #fff; }
.price-field input { border: 0; box-shadow: none; }
.price-field span { padding-right: var(--space-3); color: var(--color-text-secondary); font-weight: 800; }
.availability-row { display: flex; justify-content: space-between; align-items: center; gap: var(--space-4); flex-wrap: wrap; }
.switch { display: flex; grid-template-columns: auto; flex-direction: row; align-items: center; gap: var(--space-3); color: var(--color-primary); }
.switch input { width: 1px; height: 1px; position: absolute; opacity: 0; }
.switch span { width: 54px; height: 30px; border-radius: var(--radius-round); background: #dedbd4; position: relative; pointer-events: none; }
.switch span::after { content: ''; position: absolute; width: 24px; height: 24px; top: 3px; left: 3px; border-radius: 50%; background: #fff; transition: transform 120ms ease; box-shadow: 0 2px 5px rgba(0,0,0,0.14); }
.switch input:focus-visible + span { box-shadow: var(--shadow-focus); }
.switch input:checked + span { background: var(--color-primary); }
.switch input:checked + span::after { transform: translateX(24px); }
.form-footer { position: sticky; bottom: var(--space-4); display: flex; gap: var(--space-3); justify-content: flex-end; padding: var(--space-4); border: 1px solid var(--color-border-strong); border-radius: var(--radius-large); background: rgba(255,255,255,0.94); box-shadow: var(--shadow-card); backdrop-filter: blur(10px); }
@media (min-width: 780px) { .form-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .image-grid { grid-template-columns: minmax(0, 1fr) 220px; } .price-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 560px) { .admin-form { gap: var(--space-3); } .form-section { gap: var(--space-3); padding: var(--space-4); border-radius: 16px; } .section-heading h2 { font-size: 1rem; } .section-heading p { font-size: 0.92rem; } .form-grid, .image-grid, .price-grid { gap: var(--space-3); } .image-preview, .image-preview img { min-height: 112px; } .availability-row { align-items: flex-start; } .form-footer { position: static; flex-direction: column; } .form-footer .button { width: 100%; } }
</style>
