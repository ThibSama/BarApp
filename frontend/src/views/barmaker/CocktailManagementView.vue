<script setup lang="ts">
import { computed, ref } from 'vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import { useCatalogStore } from '@/stores/catalog';
import { formatCurrency } from '@/utils/formatters';
const catalog = useCatalogStore();
const selectedCategory = ref('all');
const filteredCocktails = computed(() => selectedCategory.value === 'all' ? catalog.cocktails : catalog.cocktails.filter((cocktail) => cocktail.categoryId === selectedCategory.value));
function remove(id: string): void { if (window.confirm('Supprimer ce cocktail ?')) catalog.deleteCocktail(id); }
</script>

<template>
  <section class="stack">
    <div class="page-title"><div><p class="eyebrow">Gestion</p><h1>Cocktails</h1></div><RouterLink class="button" to="/barmaker/cocktails/nouveau">Créer un cocktail</RouterLink></div>
    <label class="card filter-inline">Filtrer par catégorie<select v-model="selectedCategory"><option value="all">Toutes les catégories</option><option v-for="category in catalog.categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
    <div v-if="filteredCocktails.length" class="management-list"><article v-for="cocktail in filteredCocktails" :key="cocktail.id" class="card management-card"><img :src="cocktail.imageUrl" :alt="`Photo du cocktail ${cocktail.name}`" /><div><h2>{{ cocktail.name }}</h2><p>{{ cocktail.shortDescription }}</p><p class="muted">{{ catalog.getCategoryById(cocktail.categoryId)?.name }} · {{ formatCurrency(cocktail.prices.S) }} / {{ formatCurrency(cocktail.prices.M) }} / {{ formatCurrency(cocktail.prices.L) }}</p></div><StatusBadge :label="cocktail.available ? 'Disponible' : 'Indisponible'" :tone="cocktail.available ? 'success' : 'danger'" /><div class="form-actions"><RouterLink class="button secondary" :to="`/barmaker/cocktails/${cocktail.id}/modifier`">Modifier</RouterLink><button class="button secondary" type="button" @click="catalog.toggleCocktail(cocktail.id)">{{ cocktail.available ? 'Désactiver' : 'Activer' }}</button><button class="button ghost" type="button" @click="remove(cocktail.id)">Supprimer</button></div></article></div>
    <section v-else class="card empty-state"><h2>Aucun cocktail dans cette catégorie</h2><p>Créez un nouveau cocktail ou changez de filtre.</p></section>
  </section>
</template>

<style scoped>
.management-list { display: grid; gap: 1rem; }
.management-card { display: grid; gap: 1rem; align-items: center; }
.management-card img { width: 100%; height: 9rem; object-fit: cover; border-radius: 0.75rem; }
@media (min-width: 900px) { .management-card { grid-template-columns: 9rem 1fr auto auto; } }
</style>
