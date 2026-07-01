<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import CocktailImage from '@/components/common/CocktailImage.vue';
import QuantitySelector from '@/components/common/QuantitySelector.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import SuccessToast from '@/components/common/SuccessToast.vue';
import { useSuccessToast } from '@/composables/useSuccessToast';
import { useMenuStore } from '@/stores/menu';
import { useCartStore } from '@/stores/cart';
import type { ApiSize } from '@/types/api';
import { formatCurrency } from '@/utils/formatters';
import { availableSizes, priceForSize } from '@/utils/menu';

const route = useRoute();
const router = useRouter();
const menu = useMenuStore();
const cart = useCartStore();
const quantity = ref(1);
const { toastMessage, toastVisible, toastId, showSuccessToast } = useSuccessToast();

const cocktailId = computed(() => Number(route.params.id));
const cocktail = computed(() =>
  Number.isFinite(cocktailId.value) ? menu.getCocktailById(cocktailId.value) : undefined,
);
const sizeOptions = computed<ApiSize[]>(() => availableSizes(cocktail.value?.prices));
const selectedSize = ref<ApiSize>('M');
const sizeLabel: Record<ApiSize, string> = { S: 'Petit', M: 'Classique', L: 'Grand' };
const currentPrice = computed(() => {
  const unit = cocktail.value ? priceForSize(cocktail.value.prices, selectedSize.value) ?? 0 : 0;
  return unit * quantity.value;
});

// Direct reload: the public menu may not be loaded yet.
onMounted(() => {
  if (!menu.loaded) void menu.load({ initial: true });
});

// Keep the selected size valid against the cocktail's real available sizes.
watch(
  sizeOptions,
  (sizes) => {
    if (sizes.length && !sizes.includes(selectedSize.value)) selectedSize.value = sizes[0];
  },
  { immediate: true },
);

function addToCart(): void {
  if (!cocktail.value) return;
  const unitPrice = priceForSize(cocktail.value.prices, selectedSize.value);
  if (unitPrice === undefined) return;
  cart.addItem(
    { cocktailId: cocktail.value.id, name: cocktail.value.name, size: selectedSize.value, unitPrice, imageUrl: cocktail.value.imageUrl },
    quantity.value,
  );
  showSuccessToast('Le cocktail a été ajouté au panier.');
}
</script>

<template>
  <section v-if="cocktail" class="details-page">
    <RouterLink class="back-link" :to="{ name: 'client-menu' }"><AppIcon name="arrow-left" :size="18" /> Retour à la carte</RouterLink>
    <div class="details-layout">
      <figure class="details-media">
        <CocktailImage class="details-image" :image-url="cocktail.imageUrl ?? undefined" :cocktail-name="cocktail.name" />
      </figure>
      <div class="stack product-panel">
        <div class="page-title"><div><p class="eyebrow">{{ cocktail.categoryName }}</p><h1>{{ cocktail.name }}</h1><div class="title-underline" aria-hidden="true"></div></div><StatusBadge label="Disponible" tone="success" /></div>
        <p class="description">{{ cocktail.description }}</p>
        <section v-if="cocktail.ingredients.length" class="card compact-card"><h2>Ingrédients</h2><ul class="ingredient-list"><li v-for="ingredient in cocktail.ingredients" :key="ingredient.id">{{ ingredient.name }}<small v-if="ingredient.quantityLabel"> · {{ ingredient.quantityLabel }}</small></li></ul></section>
        <fieldset class="card compact-card"><legend>Choisir une taille</legend><div class="size-options"><label v-for="size in sizeOptions" :key="size" class="size-card" :class="{ selected: selectedSize === size }"><input v-model="selectedSize" type="radio" name="size" :value="size" /> <span>{{ size }}</span><small>{{ sizeLabel[size] }}</small><strong>{{ formatCurrency(priceForSize(cocktail.prices, size) ?? 0) }}</strong></label></div></fieldset>
        <div class="purchase-card card"><div><p class="eyebrow">Total</p><strong class="live-price">{{ formatCurrency(currentPrice) }}</strong></div><QuantitySelector v-model="quantity" label="Quantité du cocktail" /><button class="button" type="button" :disabled="!sizeOptions.length" @click="addToCart">Ajouter au panier</button><button class="button secondary" type="button" @click="router.push({ name: 'client-cart' })">Aller au panier</button></div>
        <SuccessToast :message="toastMessage" :visible="toastVisible" :toast-key="toastId" />
      </div>
    </div>
  </section>
  <section v-else-if="menu.loading && !menu.loaded" class="card empty-state" aria-busy="true"><h1>Chargement du cocktail…</h1></section>
  <section v-else class="card empty-state"><h1>Cocktail introuvable</h1><RouterLink class="button" :to="{ name: 'client-menu' }">Retour à la carte</RouterLink></section>
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
.ingredient-list small { color: var(--color-text-secondary); }
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
