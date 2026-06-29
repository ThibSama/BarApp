<script setup lang="ts">
import type { Cocktail } from '@/types/domain';
import { formatCurrency } from '@/utils/formatters';
import StatusBadge from '@/components/common/StatusBadge.vue';

defineProps<{ cocktail: Cocktail; categoryName: string }>();
</script>

<template>
  <article class="card cocktail-card">
    <img :src="cocktail.imageUrl" :alt="`Photo du cocktail ${cocktail.name}`" />
    <div class="card-body">
      <div class="card-heading">
        <p class="eyebrow">{{ categoryName }}</p>
        <StatusBadge :label="cocktail.available ? 'Disponible' : 'Indisponible'" :tone="cocktail.available ? 'success' : 'danger'" />
      </div>
      <h3>{{ cocktail.name }}</h3>
      <p>{{ cocktail.shortDescription }}</p>
      <p class="muted">{{ cocktail.ingredients.slice(0, 3).join(', ') }}</p>
      <div class="card-actions">
        <strong>À partir de {{ formatCurrency(cocktail.prices.S) }}</strong>
        <RouterLink class="button secondary" :to="`/client/cocktails/${cocktail.id}`">Voir le détail</RouterLink>
      </div>
    </div>
  </article>
</template>

<style scoped>
.cocktail-card { overflow: hidden; display: flex; flex-direction: column; }
img { width: 100%; height: 12rem; object-fit: cover; }
.card-body { padding: 1rem; display: grid; gap: 0.65rem; }
.card-heading, .card-actions { display: flex; align-items: center; justify-content: space-between; gap: 0.75rem; }
.card-actions { flex-wrap: wrap; }
h3 { margin: 0; }
</style>
