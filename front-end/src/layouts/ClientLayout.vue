<script setup lang="ts">
import { useCartStore } from '@/stores/cart';
const cart = useCartStore();
</script>

<template>
  <div>
    <header class="app-header">
      <RouterLink class="brand" to="/client/menu">Le Bar'app</RouterLink>
      <nav aria-label="Navigation client">
        <RouterLink to="/client/menu">Carte</RouterLink>
        <RouterLink to="/client/panier">Panier ({{ cart.itemCount }})</RouterLink>
      </nav>
    </header>
    <main class="page-shell"><RouterView /></main>
  </div>
</template>
