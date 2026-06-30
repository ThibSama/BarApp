<script setup lang="ts">
import { computed, ref } from 'vue';
import CocktailCard from '@/components/client/CocktailCard.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import { useCatalogStore } from '@/stores/catalog';
import { useCartStore } from '@/stores/cart';
import type { Cocktail, Size } from '@/types/domain';

const catalog = useCatalogStore();
const cart = useCartStore();
const selectedCategory = ref('all');
const search = ref('');
const feedback = ref('');
const hasLoaded = computed(() => catalog.cocktails.length > 0 || catalog.categories.length > 0);

function displayCategoryName(name: string): string {
  const normalized = name.toLocaleLowerCase('fr-FR');
  if (normalized.includes('classique')) return 'Classique';
  if (normalized.includes('fruit')) return 'Fruité';
  return name;
}

const filteredCocktails = computed(() => catalog.enabledCocktails.filter((cocktail) => {
  const category = catalog.getCategoryById(cocktail.categoryId);
  const matchesCategory = selectedCategory.value === 'all' || cocktail.categoryId === selectedCategory.value;
  const term = search.value.trim().toLocaleLowerCase('fr-FR');
  const haystack = [cocktail.name, cocktail.shortDescription, cocktail.ingredients.join(' ')].join(' ').toLocaleLowerCase('fr-FR');
  const matchesSearch = !term || haystack.includes(term);
  return matchesCategory && matchesSearch && category?.enabled;
}));

function addSelectedSize(cocktail: Cocktail, size: Size): void {
  if (!cocktail.available) {
    feedback.value = 'Impossible d’ajouter ce cocktail. Veuillez réessayer.';
    return;
  }
  cart.addItem(cocktail.id, size, 1);
  feedback.value = `${cocktail.name} taille ${size} a été ajouté au panier.`;
}
</script>

<template>
  <section class="menu-page stack">
    <div class="page-title menu-hero">
      <div>
        <h1>Notre carte</h1>
        <div class="title-underline" aria-hidden="true"></div>
        <p class="hero-subtitle">Découvrez tous nos cocktails</p>
      </div>
      <RouterLink class="basket-button" to="/client/panier"><span aria-hidden="true"><AppIcon name="clipboard-list" :size="20" /></span> Panier <span class="basket-count">{{ cart.itemCount }}</span></RouterLink>
    </div>

    <section class="surface-panel menu-controls" aria-label="Recherche et catégories">
      <form class="filters" role="search" @submit.prevent>
        <label class="search-field">
          <span class="visually-hidden">Rechercher</span>
          <span class="search-input-wrap">
            <span aria-hidden="true"><AppIcon name="search" :size="22" /></span>
            <input v-model="search" type="search" placeholder="Rechercher un cocktail ou un ingrédient…" autocomplete="off" />
            <button v-if="search" class="clear-search" type="button" aria-label="Effacer la recherche" @click="search = ''"><AppIcon name="x" :size="18" /></button>
          </span>
        </label>
      </form>

      <div class="category-pills" aria-label="Catégories">
        <button type="button" :class="{ active: selectedCategory === 'all' }" :aria-pressed="selectedCategory === 'all'" @click="selectedCategory = 'all'">Tous</button>
        <button v-for="category in catalog.enabledCategories" :key="category.id" type="button" :class="{ active: selectedCategory === category.id }" :aria-pressed="selectedCategory === category.id" @click="selectedCategory = category.id">{{ displayCategoryName(category.name) }}</button>
      </div>
    </section>

    <p class="visually-hidden" aria-live="polite">{{ feedback }}</p>
    <p v-if="feedback" class="alert success" role="status">{{ feedback }}</p>
    <p v-if="!hasLoaded" class="alert warning" role="status">Chargement de la carte…</p>

    <div v-if="filteredCocktails.length" class="card-grid">
      <CocktailCard v-for="cocktail in filteredCocktails" :key="cocktail.id" :cocktail="cocktail" :category-name="displayCategoryName(catalog.getCategoryById(cocktail.categoryId)?.name ?? 'Catégorie')" @add="addSelectedSize" />
    </div>
    <section v-else-if="hasLoaded" class="card empty-state"><h2>Aucun cocktail trouvé</h2><p>Essayez un autre ingrédient ou une autre catégorie.</p><button v-if="search || selectedCategory !== 'all'" class="button secondary" type="button" @click="search = ''; selectedCategory = 'all'">Réinitialiser les filtres</button></section>
  </section>
</template>

<style scoped>
.menu-page { gap: 30px; }
.menu-hero { align-items: flex-start; margin-bottom: 4px; }
.hero-subtitle { color: var(--color-text-secondary); font-size: 1.05rem; margin-bottom: 0; }
.basket-button { align-self: center; display: inline-flex; align-items: center; gap: 10px; min-height: 50px; padding: 0 18px; border-radius: 14px; background: var(--color-primary); color: #fff; font-weight: 800; box-shadow: 0 8px 18px rgba(29,43,31,0.12); }
.basket-button:hover { text-decoration: none; background: var(--color-primary-hover); }
.basket-count { display: inline-grid; place-items: center; min-width: 26px; height: 26px; padding: 0 7px; border-radius: var(--radius-round); background: #fff; color: var(--color-primary); font-size: 0.78rem; }
.menu-controls { display: grid; gap: var(--space-5); }
.search-field { gap: var(--space-3); }
.search-input-wrap { position: relative; display: flex; align-items: center; }
.search-input-wrap > span { position: absolute; left: 22px; z-index: 1; color: var(--color-text-secondary); font-size: 1.35rem; }
.search-input-wrap input { height: 68px; padding-left: 58px; padding-right: 3rem; font-size: 1.04rem; border-radius: 18px; box-shadow: 0 10px 28px rgba(29,43,31,0.055); }
.clear-search { position: absolute; right: var(--space-2); width: 44px; height: 44px; border: 0; border-radius: var(--radius-round); background: transparent; color: var(--color-text-secondary); cursor: pointer; display: grid; place-items: center; }
.category-pills { gap: 12px; margin-top: 2px; }
.category-pills button { display: inline-flex; align-items: center; gap: 9px; min-height: 48px; padding: 0 18px; border-radius: 14px; font-size: 0.94rem; box-shadow: 0 3px 12px rgba(0,0,0,0.025); }
.category-pills button span { color: currentColor; }
@media (max-width: 720px) {
  .basket-button { display: none; }
  .category-pills { flex-wrap: nowrap; overflow-x: auto; padding-bottom: var(--space-2); scrollbar-width: thin; }
  .category-pills button { flex: 0 0 auto; }
}
</style>
