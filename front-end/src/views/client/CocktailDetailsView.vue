<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import QuantitySelector from '@/components/common/QuantitySelector.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
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

function addToCart(): void {
  if (!cocktail.value || !cocktail.value.available) return;
  cart.addItem(cocktail.value.id, selectedSize.value, quantity.value);
  feedback.value = 'Cocktail ajouté au panier.';
}
</script>

<template>
  <section v-if="cocktail" class="details-layout">
    <img class="details-image" :src="cocktail.imageUrl" :alt="`Photo du cocktail ${cocktail.name}`" />
    <div class="stack">
      <RouterLink to="/client/menu">← Retour à la carte</RouterLink>
      <div class="page-title"><div><p class="eyebrow">{{ catalog.getCategoryById(cocktail.categoryId)?.name }}</p><h1>{{ cocktail.name }}</h1></div><StatusBadge :label="cocktail.available ? 'Disponible' : 'Indisponible'" :tone="cocktail.available ? 'success' : 'danger'" /></div>
      <p>{{ cocktail.description }}</p>
      <section class="card"><h2>Ingrédients</h2><ul><li v-for="ingredient in cocktail.ingredients" :key="ingredient">{{ ingredient }}</li></ul></section>
      <fieldset class="card"><legend>Choisir une taille</legend><div class="size-options"><label v-for="size in ['S','M','L']" :key="size"><input v-model="selectedSize" type="radio" name="size" :value="size" /> {{ size }} — {{ formatCurrency(cocktail.prices[size as Size]) }}</label></div></fieldset>
      <div class="purchase-row"><QuantitySelector v-model="quantity" label="Quantité" /><button class="button" type="button" :disabled="!cocktail.available" @click="addToCart">Ajouter au panier</button><button class="button secondary" type="button" @click="router.push('/client/panier')">Aller au panier</button></div>
      <p v-if="!cocktail.available" class="alert error">Ce cocktail est actuellement indisponible.</p>
      <p v-if="feedback" class="alert success">{{ feedback }}</p>
    </div>
  </section>
  <section v-else class="card empty-state"><h1>Cocktail introuvable</h1><RouterLink class="button" to="/client/menu">Retour à la carte</RouterLink></section>
</template>

<style scoped>
.details-layout { display: grid; gap: 1.5rem; }
.details-image { width: 100%; max-height: 28rem; object-fit: cover; border-radius: 1rem; }
.size-options, .purchase-row { display: flex; gap: 1rem; flex-wrap: wrap; align-items: center; }
@media (min-width: 900px) { .details-layout { grid-template-columns: 1fr 1fr; align-items: start; } }
</style>
