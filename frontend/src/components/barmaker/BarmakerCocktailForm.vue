<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import CocktailImage from '@/components/common/CocktailImage.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useAdminCocktailsStore } from '@/stores/adminCocktails';
import { useAdminCategoriesStore } from '@/stores/adminCategories';
import { useAdminIngredientsStore } from '@/stores/adminIngredients';
import type { ApiSize, CocktailRequest } from '@/types/api';
import { describeAdminError, isConflict } from '@/utils/adminErrors';
import { formatCurrency } from '@/utils/formatters';
import { ApiError } from '@/services/apiClient';

const props = withDefaults(
  defineProps<{ cocktailId?: number; formId?: string; showFooter?: boolean; cancelLabel?: string }>(),
  { formId: 'cocktail-form', showFooter: true, cancelLabel: 'Annuler' },
);

const emit = defineEmits<{ saved: []; cancel: []; 'submitting-change': [value: boolean] }>();

const cocktails = useAdminCocktailsStore();
const categories = useAdminCategoriesStore();
const ingredientsStore = useAdminIngredientsStore();

const SIZES: ApiSize[] = ['S', 'M', 'L'];

interface IngredientRow {
  name: string;
  quantityLabel: string;
}

const form = reactive({
  categoryId: null as number | null,
  name: '',
  description: '',
  imageUrl: '',
  active: true,
  ingredients: [{ name: '', quantityLabel: '' }] as IngredientRow[],
  prices: { S: 5, M: 7, L: 9 } as Record<ApiSize, number>,
});

const submitting = ref(false);
const loadingDetail = ref(false);
const formError = ref('');
const errors = reactive<Record<string, string>>({});

const activeCategories = computed(() => categories.items.filter((category) => category.active));
const ingredientSuggestions = computed(() => ingredientsStore.activeItems.map((item) => item.name));

onMounted(async () => {
  await Promise.all([
    categories.loaded ? Promise.resolve() : categories.load({ initial: true }),
    ingredientsStore.loaded ? Promise.resolve() : ingredientsStore.load({ initial: true }),
  ]);
  if (props.cocktailId) await loadExisting(props.cocktailId);
  else form.categoryId = activeCategories.value[0]?.id ?? null;
});

async function loadExisting(id: number): Promise<void> {
  loadingDetail.value = true;
  try {
    const data = await cocktails.loadOne(id);
    form.categoryId = data.categoryId;
    form.name = data.name;
    form.description = data.description;
    form.imageUrl = data.imageUrl ?? '';
    form.active = data.active;
    form.ingredients = data.ingredients.length
      ? data.ingredients.map((ingredient) => ({
          name: ingredient.name,
          quantityLabel: ingredient.quantityLabel ?? '',
        }))
      : [{ name: '', quantityLabel: '' }];
    SIZES.forEach((size) => {
      const price = data.prices.find((entry) => entry.size === size);
      if (price) form.prices[size] = price.price;
    });
  } catch (err) {
    formError.value = describeAdminError(err);
  } finally {
    loadingDetail.value = false;
  }
}

function addIngredient(): void {
  form.ingredients.push({ name: '', quantityLabel: '' });
}
function removeIngredient(index: number): void {
  form.ingredients.splice(index, 1);
  if (form.ingredients.length === 0) form.ingredients.push({ name: '', quantityLabel: '' });
}

function validate(): boolean {
  Object.keys(errors).forEach((key) => delete errors[key]);
  if (form.categoryId === null) errors.categoryId = 'La catégorie est obligatoire.';
  if (!form.name.trim()) errors.name = 'Le nom est obligatoire.';
  if (!form.description.trim()) errors.description = 'La description est obligatoire.';

  const filled = form.ingredients.map((ingredient) => ingredient.name.trim()).filter(Boolean);
  if (filled.length === 0) errors.ingredients = 'Au moins un ingrédient est obligatoire.';
  const lowered = filled.map((name) => name.toLocaleLowerCase('fr-FR'));
  if (new Set(lowered).size !== lowered.length) {
    errors.ingredients = 'Chaque ingrédient doit être unique (sans doublon de nom).';
  }

  SIZES.forEach((size) => {
    const price = form.prices[size];
    if (!Number.isFinite(price) || price <= 0) errors[`price${size}`] = `Le prix ${size} doit être supérieur à 0.`;
  });

  return Object.keys(errors).length === 0;
}

function buildPayload(): CocktailRequest {
  return {
    categoryId: form.categoryId as number,
    name: form.name.trim(),
    description: form.description.trim(),
    imageUrl: form.imageUrl.trim() || null,
    active: form.active,
    ingredients: form.ingredients
      .map((ingredient, index) => ({
        name: ingredient.name.trim(),
        quantityLabel: ingredient.quantityLabel.trim() || null,
        displayOrder: index,
      }))
      .filter((ingredient) => ingredient.name.length > 0),
    prices: SIZES.map((size) => ({ size, price: form.prices[size] })),
  };
}

function applyFieldErrors(err: unknown): void {
  if (err instanceof ApiError && err.fieldErrors?.length) {
    err.fieldErrors.forEach((field) => {
      // Map top-level scalar fields; nested errors fall back to the banner.
      if (['name', 'description', 'imageUrl', 'categoryId'].includes(field.field)) {
        errors[field.field] = field.message;
      }
    });
  }
}

async function save(): Promise<void> {
  formError.value = '';
  if (!validate() || submitting.value) return;
  submitting.value = true;
  emit('submitting-change', true);
  try {
    const payload = buildPayload();
    if (props.cocktailId) await cocktails.update(props.cocktailId, payload);
    else await cocktails.create(payload);
    emit('saved');
  } catch (err) {
    if (isConflict(err)) errors.name = 'Un cocktail portant ce nom existe déjà dans cette catégorie.';
    applyFieldErrors(err);
    formError.value = describeAdminError(err);
  } finally {
    submitting.value = false;
    emit('submitting-change', false);
  }
}

defineExpose({ save, submitting });
</script>

<template>
  <form :id="formId" class="admin-form" @submit.prevent="save">
    <p v-if="loadingDetail" class="alert warning" role="status">Chargement du cocktail…</p>
    <p v-if="formError" class="alert error" role="alert">{{ formError }}</p>

    <section class="form-section">
      <div class="section-heading"><h2>Informations générales</h2><p class="muted">Nom, description et catégorie d’affichage.</p></div>
      <div class="form-grid">
        <label>Nom <input v-model="form.name" type="text" /><span v-if="errors.name" class="field-error">{{ errors.name }}</span></label>
        <label class="wide">Description <textarea v-model="form.description" rows="4"></textarea><span v-if="errors.description" class="field-error">{{ errors.description }}</span></label>
        <label>Catégorie
          <select v-model="form.categoryId">
            <option :value="null" disabled>Sélectionner…</option>
            <option v-for="category in activeCategories" :key="category.id" :value="category.id">{{ category.name }}</option>
          </select>
          <span v-if="errors.categoryId" class="field-error">{{ errors.categoryId }}</span>
        </label>
      </div>
    </section>

    <section class="form-section">
      <div class="section-heading"><h2>Image</h2><p class="muted">URL utilisée par la carte client et l’espace Barmaker.</p></div>
      <div class="image-grid">
        <label>URL de l’image <input v-model="form.imageUrl" type="url" /><span v-if="errors.imageUrl" class="field-error">{{ errors.imageUrl }}</span></label>
        <div class="image-preview"><CocktailImage :image-url="form.imageUrl || undefined" :cocktail-name="form.name || 'en cours de création'" /></div>
      </div>
    </section>

    <section class="form-section">
      <div class="section-heading"><h2>Ingrédients</h2><p class="muted">Réutilisez un ingrédient existant ou saisissez un nouveau nom. Les lignes vides sont ignorées.</p></div>
      <datalist id="ingredient-suggestions"><option v-for="name in ingredientSuggestions" :key="name" :value="name" /></datalist>
      <div class="ingredient-list">
        <div v-for="(row, index) in form.ingredients" :key="index" class="ingredient-row">
          <label>Ingrédient {{ index + 1 }}<input v-model="row.name" type="text" list="ingredient-suggestions" /></label>
          <label>Quantité<input v-model="row.quantityLabel" type="text" placeholder="ex. 4 cl" /></label>
          <button class="icon-button danger" type="button" :aria-label="`Retirer l’ingrédient ${index + 1}`" @click="removeIngredient(index)"><AppIcon name="trash" :size="18" /></button>
        </div>
      </div>
      <button class="admin-action secondary add-ingredient" type="button" @click="addIngredient"><AppIcon name="plus" :size="17" />Ajouter un ingrédient</button>
      <span v-if="errors.ingredients" class="field-error">{{ errors.ingredients }}</span>
    </section>

    <section class="form-section">
      <div class="section-heading"><h2>Tailles et prix</h2><p class="muted">Les trois tailles S, M et L sont obligatoires.</p></div>
      <div class="price-grid">
        <label v-for="size in SIZES" :key="size">Prix {{ size }} <span class="price-field"><input v-model.number="form.prices[size]" type="number" min="0" step="0.5" /><span>€</span></span><small>{{ formatCurrency(form.prices[size] || 0) }}</small><span v-if="errors[`price${size}`]" class="field-error">{{ errors[`price${size}`] }}</span></label>
      </div>
    </section>

    <section class="form-section availability-row">
      <div class="section-heading"><h2>Disponibilité</h2><p class="muted">Un cocktail désactivé disparaît de la carte client.</p></div>
      <label class="switch"><input v-model="form.active" type="checkbox" /><span></span>Cocktail disponible</label>
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
.ingredient-row { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr) 44px; gap: var(--space-3); align-items: end; }
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
@media (max-width: 560px) { .admin-form { gap: var(--space-3); } .form-section { gap: var(--space-3); padding: var(--space-4); border-radius: 16px; } .section-heading h2 { font-size: 1rem; } .section-heading p { font-size: 0.92rem; } .form-grid, .image-grid, .price-grid { gap: var(--space-3); } .image-preview, .image-preview img { min-height: 112px; } .ingredient-row { grid-template-columns: minmax(0, 1fr) 44px; } .ingredient-row label:nth-child(2) { grid-column: 1; } .availability-row { align-items: flex-start; } .form-footer { position: static; flex-direction: column; } .form-footer .button { width: 100%; } }
</style>
