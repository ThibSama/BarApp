<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ApiSize } from '@/types/api';
import type { MenuCocktailView } from '@/stores/menu';
import CocktailImage from '@/components/common/CocktailImage.vue';
import { formatCurrency } from '@/utils/formatters';
import { availableSizes, priceForSize } from '@/utils/menu';

const props = defineProps<{ cocktail: MenuCocktailView; categoryName: string }>();
const emit = defineEmits<{ add: [cocktail: MenuCocktailView, size: ApiSize] }>();

const ingredientSummary = computed(() => {
  const names = props.cocktail.ingredients.map((ingredient) => ingredient.name);
  return names.slice(0, 4).join(', ') || props.cocktail.description;
});

// Only the sizes the backend actually returns are offered.
const sizeOptions = computed<ApiSize[]>(() => availableSizes(props.cocktail.prices));
const selectedSize = ref<ApiSize>(sizeOptions.value[0] ?? 'M');
const selectedPrice = computed(() => priceForSize(props.cocktail.prices, selectedSize.value) ?? 0);
const canAdd = computed(() => sizeOptions.value.length > 0);
</script>

<template>
  <article class="cocktail-card">
    <RouterLink class="card-link" :to="{ name: 'client-cocktail-details', params: { id: cocktail.id } }" :aria-label="`Voir le détail de ${cocktail.name}`">
      <CocktailImage :image-url="cocktail.imageUrl ?? undefined" :cocktail-name="cocktail.name" />
    </RouterLink>
    <div class="card-body">
      <p class="category-line">{{ categoryName }}</p>
      <h3>{{ cocktail.name }}</h3>
      <p class="ingredients">{{ ingredientSummary }}</p>
      <fieldset v-if="canAdd" class="card-size-selector" :aria-label="`Choisir la taille de ${cocktail.name}`">
        <legend class="visually-hidden">Taille</legend>
        <label v-for="size in sizeOptions" :key="size" :class="{ selected: selectedSize === size }">
          <input v-model="selectedSize" type="radio" :name="`size-${cocktail.id}`" :value="size" />
          <span>{{ size }}</span>
        </label>
      </fieldset>
      <div class="card-actions">
        <strong>{{ formatCurrency(selectedPrice) }}</strong>
        <button class="button icon add-button" type="button" :disabled="!canAdd" :aria-label="`Ajouter ${cocktail.name} taille ${selectedSize} au panier`" @click="emit('add', cocktail, selectedSize)">+</button>
      </div>
    </div>
  </article>
</template>

<style scoped>
.cocktail-card { position: relative; overflow: hidden; display: grid; min-height: 358px; background: var(--color-surface); border: 1px solid rgba(229,219,204,0.9); border-radius: 22px; box-shadow: var(--shadow-card-soft); transition: transform 160ms ease, box-shadow 160ms ease; }
.cocktail-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-card); }
.card-link { position: relative; display: block; overflow: hidden; min-height: 190px; background: #f8f4ee; }
.card-link :deep(.cocktail-image) { position: absolute; inset: 0; z-index: 1; }
.card-body { padding: 18px 18px 16px; display: grid; gap: 10px; }
.category-line { display: flex; align-items: center; gap: 7px; margin: 0; color: var(--color-text-secondary); font-size: 0.8rem; font-weight: 750; }
h3 { margin: 0; font-size: 1.28rem; letter-spacing: -0.025em; }
.ingredients { min-height: 2.85em; margin: 0; color: var(--color-text-secondary); line-height: 1.42; font-size: 0.92rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-size-selector { display: inline-grid; grid-template-columns: repeat(3, 1fr); gap: 4px; margin: 0; padding: 3px; border: 1px solid rgba(229,219,204,0.92); border-radius: 13px; background: #fbfaf7; }
.card-size-selector label { min-width: 0; display: grid; place-items: center; min-height: 34px; border-radius: 10px; color: var(--color-text-secondary); font-size: 0.82rem; font-weight: 850; cursor: pointer; }
.card-size-selector label.selected { background: var(--color-primary); color: #fff; box-shadow: 0 4px 12px rgba(29,43,31,0.12); }
.card-size-selector input { position: absolute; width: 1px; height: 1px; opacity: 0; pointer-events: none; }
.card-size-selector label:has(input:focus-visible) { box-shadow: var(--shadow-focus); }
.card-actions { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-top: 2px; }
.card-actions strong { font-size: 1.08rem; }
.add-button { flex: 0 0 auto; border: 0; background: #f0ece4; color: var(--color-primary); font-size: 1.45rem; line-height: 1; }
.add-button:hover { background: var(--color-primary); color: #fff; }
@media (max-width: 639px) {
  .cocktail-card { grid-template-columns: 104px minmax(0, 1fr); min-height: 132px; }
  .card-link { min-height: 100%; }
  .card-body { padding: var(--space-3); gap: 8px; }
  .card-size-selector { max-width: 9.5rem; }
  .card-size-selector label { min-height: 30px; }
  .card-actions strong { font-size: 0.95rem; }
}
</style>
