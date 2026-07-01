<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import CocktailCard from '@/components/client/CocktailCard.vue';
import AppIcon from '@/components/common/AppIcon.vue';
import SuccessToast from '@/components/common/SuccessToast.vue';
import { useSuccessToast } from '@/composables/useSuccessToast';
import { useMenuStore, type MenuCocktailView } from '@/stores/menu';
import { useCartStore } from '@/stores/cart';
import type { ApiSize } from '@/types/api';
import { priceForSize } from '@/utils/menu';

const menu = useMenuStore();
const cart = useCartStore();
const search = ref('');
const { toastMessage, toastVisible, toastId, showSuccessToast } = useSuccessToast();

onMounted(() => menu.load({ initial: true }));

function displayCategoryName(name: string): string {
  const normalized = name.toLocaleLowerCase('fr-FR');
  if (normalized.includes('classique')) return 'Classique';
  if (normalized.includes('fruit')) return 'Fruité';
  return name;
}

const filteredCocktails = computed(() =>
  menu.cocktails.filter((cocktail) => {
    const matchesCategory =
      menu.selectedCategoryId === 'all' || cocktail.categoryId === menu.selectedCategoryId;
    const term = search.value.trim().toLocaleLowerCase('fr-FR');
    const haystack = [cocktail.name, cocktail.description, cocktail.ingredients.map((i) => i.name).join(' ')]
      .join(' ')
      .toLocaleLowerCase('fr-FR');
    const matchesSearch = !term || haystack.includes(term);
    return matchesCategory && matchesSearch;
  }),
);

function addSelectedSize(cocktail: MenuCocktailView, size: ApiSize): void {
  const unitPrice = priceForSize(cocktail.prices, size);
  if (unitPrice === undefined) return;
  cart.addItem({ cocktailId: cocktail.id, name: cocktail.name, size, unitPrice, imageUrl: cocktail.imageUrl }, 1);
  showSuccessToast(`${cocktail.name} taille ${size} a été ajouté au panier.`);
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
      <RouterLink class="basket-button" :to="{ name: 'client-cart' }"><span aria-hidden="true"><AppIcon name="clipboard-list" :size="20" /></span> Panier <span class="basket-count">{{ cart.itemCount }}</span></RouterLink>
    </div>

    <!-- Initial loading: no menu data yet. -->
    <p v-if="menu.loading && !menu.loaded" class="alert warning" role="status">Chargement de la carte…</p>

    <!-- Hard error with no data to show: offer a retry, never fake data. -->
    <section v-else-if="menu.error && !menu.cocktails.length" class="card empty-state">
      <h2>Carte indisponible</h2>
      <p>{{ menu.error }}</p>
      <button class="button" type="button" @click="menu.retry()">Réessayer</button>
    </section>

    <template v-else>
      <!-- Transient refresh failure: keep the last menu, warn softly. -->
      <p v-if="menu.error" class="alert warning" role="status">{{ menu.error }}</p>

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
          <button type="button" :class="{ active: menu.selectedCategoryId === 'all' }" :aria-pressed="menu.selectedCategoryId === 'all'" @click="menu.selectedCategoryId = 'all'">Tous</button>
          <button v-for="category in menu.categories" :key="category.id" type="button" :class="{ active: menu.selectedCategoryId === category.id }" :aria-pressed="menu.selectedCategoryId === category.id" @click="menu.selectedCategoryId = category.id">{{ displayCategoryName(category.name) }}</button>
        </div>
      </section>

      <SuccessToast :message="toastMessage" :visible="toastVisible" :toast-key="toastId" />

      <div v-if="filteredCocktails.length" class="card-grid">
        <CocktailCard v-for="cocktail in filteredCocktails" :key="cocktail.id" :cocktail="cocktail" :category-name="displayCategoryName(cocktail.categoryName)" @add="addSelectedSize" />
      </div>
      <section v-else class="card empty-state"><h2>Aucun cocktail trouvé</h2><p>Essayez un autre ingrédient ou une autre catégorie.</p><button v-if="search || menu.selectedCategoryId !== 'all'" class="button secondary" type="button" @click="search = ''; menu.selectedCategoryId = 'all'">Réinitialiser les filtres</button></section>
    </template>
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
