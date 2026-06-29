<script setup lang="ts">
import { computed, ref } from 'vue';
import CocktailCard from '@/components/client/CocktailCard.vue';
import { useCatalogStore } from '@/stores/catalog';

const catalog = useCatalogStore();
const selectedCategory = ref('all');
const search = ref('');

const filteredCocktails = computed(() => catalog.cocktails.filter((cocktail) => {
  const category = catalog.getCategoryById(cocktail.categoryId);
  const matchesCategory = selectedCategory.value === 'all' || cocktail.categoryId === selectedCategory.value;
  const term = search.value.trim().toLowerCase();
  const matchesSearch = !term || [cocktail.name, cocktail.shortDescription, cocktail.ingredients.join(' ')].join(' ').toLowerCase().includes(term);
  return matchesCategory && matchesSearch && category?.enabled;
}));
</script>

<template>
  <section class="stack">
    <div class="page-title"><div><p class="eyebrow">Espace client</p><h1>Carte des cocktails</h1></div><RouterLink class="button" to="/client/panier">Voir le panier</RouterLink></div>
    <form class="filters card" @submit.prevent>
      <label>Rechercher un cocktail<input v-model="search" type="search" placeholder="Menthe, mangue, mojito…" /></label>
      <label>Filtrer par catégorie<select v-model="selectedCategory"><option value="all">Toutes les catégories</option><option v-for="category in catalog.enabledCategories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
    </form>
    <div class="category-pills" aria-label="Catégories">
      <button type="button" :class="{ active: selectedCategory === 'all' }" @click="selectedCategory = 'all'">Toutes</button>
      <button v-for="category in catalog.enabledCategories" :key="category.id" type="button" :class="{ active: selectedCategory === category.id }" @click="selectedCategory = category.id">{{ category.name }}</button>
    </div>
    <div v-if="filteredCocktails.length" class="card-grid">
      <CocktailCard v-for="cocktail in filteredCocktails" :key="cocktail.id" :cocktail="cocktail" :category-name="catalog.getCategoryById(cocktail.categoryId)?.name ?? 'Catégorie'" />
    </div>
    <section v-else class="card empty-state"><h2>Aucun cocktail trouvé</h2><p>Essayez une autre recherche ou une autre catégorie.</p></section>
  </section>
</template>
