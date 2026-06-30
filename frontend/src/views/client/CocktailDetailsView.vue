<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CocktailImage from '@/components/common/CocktailImage.vue';
import QuantitySelector from '@/components/common/QuantitySelector.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useCatalogStore } from '@/stores/catalog';
import { useCartStore } from '@/stores/cart';
import type { Size } from '@/types/domain';
import { formatCurrency } from '@/utils/formatters';

const route = useRoute();
const router = useRouter();
const catalog = useCatalogStore();
const cart = useCartStore();
const selectedSize = ref<Size>('M');
const quantity = ref(1);
const feedback = ref('');
const cocktail = computed(() => catalog.getCocktailById(String(route.params.id)));
const categoryName = computed(() => cocktail.value ? catalog.getCategoryById(cocktail.value.categoryId)?.name : 'Catégorie');
const sizeOptions = computed<Size[]>(() => ['S', 'M', 'L'].filter((size) => cocktail.value?.prices[size as Size] !== undefined) as Size[]);
const currentPrice = computed(() => cocktail.value ? cocktail.value.prices[selectedSize.value] * quantity.value : 0);

function addToCart(): void {
  if (!cocktail.value || !cocktail.value.available) return;
  cart.addItem(cocktail.value.id, selectedSize.value, quantity.value);
  feedback.value = 'Le cocktail a été ajouté au panier.';
}
</script>

<template>
  <section v-if="cocktail" class="details-page">
    <RouterLink class="back-link" to="/client/menu"><AppIcon name="arrow-left" :size="18" /> Retour à la carte</RouterLink>
    <div class="details-layout">
      <figure class="details-media">
        <CocktailImage class="details-image" :image-url="cocktail.imageUrl" :cocktail-name="cocktail.name" />
      </figure>
      <div class="stack product-panel">
        <div class="page-title"><div><p class="eyebrow">{{ categoryName }}</p><h1>{{ cocktail.name }}</h1><div class="title-underline" aria-hidden="true"></div></div><StatusBadge :label="cocktail.available ? 'Disponible' : 'Indisponible'" :tone="cocktail.available ? 'success' : 'danger'" /></div>
        <p class="description">{{ cocktail.description }}</p>
        <section v-if="cocktail.ingredients.length" class="card compact-card"><h2>Ingrédients</h2><ul class="ingredient-list"><li v-for="ingredient in cocktail.ingredients" :key="ingredient">{{ ingredient }}</li></ul></section>
        <fieldset class="card compact-card"><legend>Choisir une taille</legend><div class="size-options"><label v-for="size in sizeOptions" :key="size" class="size-card" :class="{ selected: selectedSize === size }"><input v-model="selectedSize" type="radio" name="size" :value="size" /> <span>{{ size }}</span><small>{{ size === 'S' ? 'Petit' : size === 'M' ? 'Classique' : 'Grand' }}</small><strong>{{ formatCurrency(cocktail.prices[size]) }}</strong></label></div></fieldset>
        <div class="purchase-card card"><div><p class="eyebrow">Total</p><strong class="live-price">{{ formatCurrency(currentPrice) }}</strong></div><QuantitySelector v-model="quantity" label="Quantité du cocktail" /><button class="button" type="button" :disabled="!cocktail.available" @click="addToCart">Ajouter au panier</button><button class="button secondary" type="button" @click="router.push('/client/panier')">Aller au panier</button></div>
        <p v-if="!cocktail.available" class="alert error">Ce cocktail est actuellement indisponible.</p>
        <p class="visually-hidden" aria-live="polite">{{ feedback }}</p>
        <p v-if="feedback" class="alert success" role="status">{{ feedback }}</p>
      </div>
    </div>
  </section>
  <section v-else class="card empty-state"><h1>Cocktail introuvable</h1><RouterLink class="button" to="/client/menu">Retour à la carte</RouterLink></section>
</template>

<style scoped>
.details-page { display: grid; gap: 30px; }
.back-link { width: fit-content; }
.details-layout { display: grid; gap: var(--space-8); }
.details-media { position: relative; min-height: 22rem; margin: 0; overflow: hidden; border-radius: 26px; background: linear-gradient(135deg, #faf8f2, #ece6dc); box-shadow: var(--shadow-card); }
.details-image { position: absolute; inset: 0; z-index: 1; width: 100%; height: 100%; object-fit: cover; }
.product-panel { align-content: start; }
.description { color: var(--color-text-secondary); line-height: 1.7; font-size: 1.08rem; }
.compact-card { box-shadow: var(--shadow-card-soft); }
.ingredient-list { margin: 0; padding-left: 1.2rem; columns: 2; color: var(--color-text-secondary); }
.size-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-3); }
.size-card { position: relative; display: grid; gap: var(--space-1); justify-items: center; text-align: center; border: 1px solid var(--color-border); border-radius: var(--radius-medium); padding: var(--space-3); cursor: pointer; min-height: 96px; }
.size-card input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.size-card span { font-weight: 900; font-size: 1.2rem; }
.size-card small { color: var(--color-text-secondary); }
.size-card.selected { background: var(--color-primary); color: #fff; border-color: var(--color-primary); box-shadow: inset 0 0 0 2px rgba(255,255,255,0.18); }
.size-card.selected small { color: rgba(255,255,255,0.74); }
.purchase-card { display: flex; gap: var(--space-4); align-items: center; flex-wrap: wrap; }
.live-price { font-size: 1.6rem; }
@media (min-width: 900px) { .details-layout { grid-template-columns: minmax(0, 0.9fr) minmax(0, 1fr); align-items: start; } .details-media { position: sticky; top: var(--space-8); min-height: 34rem; } }
@media (max-width: 520px) { .size-options { grid-template-columns: repeat(3, minmax(88px, 1fr)); overflow-x: auto; } .purchase-card .button { width: 100%; } .ingredient-list { columns: 1; } }
</style>
