<script setup lang="ts">
import { useCartStore } from '@/stores/cart';
import AppIcon from '@/components/common/AppIcon.vue';
const cart = useCartStore();
</script>

<template>
  <div class="client-shell">
    <aside class="client-sidebar" aria-label="Navigation client bureau">
      <RouterLink class="client-logo" to="/client/menu" aria-label="Le Bar’App - carte des cocktails">
        <span aria-hidden="true"><AppIcon name="martini" :size="28" /></span>
        <strong>LE BAR’APP</strong>
        <small>COCKTAILS &amp; GOOD VIBES</small>
      </RouterLink>
      <nav class="client-nav" aria-label="Navigation client">
        <RouterLink to="/client/menu"><span><span aria-hidden="true"><AppIcon name="martini" :size="20" /></span> Carte des cocktails</span></RouterLink>
        <RouterLink to="/client/panier"><span><span aria-hidden="true"><AppIcon name="clipboard-list" :size="20" /></span> Panier</span> <span v-if="cart.itemCount" class="nav-badge">{{ cart.itemCount }}</span></RouterLink>
        <RouterLink to="/client/suivi"><span><span aria-hidden="true"><AppIcon name="clock" :size="20" /></span> Ma commande</span></RouterLink>
      </nav>
      <div class="sidebar-illustration" aria-hidden="true"><AppIcon name="martini" :size="96" /></div>
    </aside>

    <div class="client-content">
      <header class="mobile-client-header">
        <RouterLink class="mobile-brand" to="/client/menu" aria-label="Le Bar’App">LE BAR’APP</RouterLink>
        <RouterLink class="basket-shortcut" to="/client/panier" aria-label="Ouvrir le panier">
          Panier <span class="nav-badge">{{ cart.itemCount }}</span>
        </RouterLink>
      </header>
      <main class="client-page-shell"><RouterView /></main>
    </div>

    <nav class="mobile-bottom-nav" aria-label="Navigation client mobile">
      <RouterLink to="/client/menu">Carte</RouterLink>
      <RouterLink to="/client/panier">Panier <span v-if="cart.itemCount" class="nav-badge">{{ cart.itemCount }}</span></RouterLink>
      <RouterLink to="/client/suivi">Ma commande</RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.client-shell { min-height: 100vh; background: transparent; }
.client-sidebar { display: none; }
.client-content { min-width: 0; }
.client-page-shell { width: min(1180px, calc(100% - 2rem)); margin: 0 auto; padding: calc(var(--space-6) + env(safe-area-inset-top)) 0 calc(6rem + env(safe-area-inset-bottom)); }
.mobile-client-header { position: sticky; top: 0; z-index: 20; display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); min-height: calc(60px + env(safe-area-inset-top)); padding: calc(var(--space-3) + env(safe-area-inset-top)) var(--space-4) var(--space-3); background: rgba(251,248,242,0.94); border-bottom: 1px solid rgba(229,219,204,0.84); backdrop-filter: blur(14px); }
.mobile-brand { color: var(--color-primary); letter-spacing: 0.08em; font-weight: 900; }
.basket-shortcut { display: inline-flex; align-items: center; gap: var(--space-2); min-height: 44px; color: var(--color-primary); }
.client-logo { display: grid; justify-items: start; gap: var(--space-2); color: var(--color-primary); text-decoration: none; }
.client-logo span { width: 54px; height: 54px; border: 1px solid rgba(229,219,204,0.9); border-radius: var(--radius-large); display: grid; place-items: center; background: #fff; color: var(--color-primary); font-size: 1.55rem; box-shadow: var(--shadow-card-soft); }
.client-logo strong { font-family: var(--font-heading); font-size: 1.38rem; letter-spacing: 0.06em; }
.client-logo small { color: var(--color-text-secondary); font-size: 0.68rem; letter-spacing: 0.15em; font-weight: 800; }
.client-nav { display: grid; gap: var(--space-2); }
.client-nav a { display: flex; align-items: center; justify-content: space-between; min-height: 52px; padding: 0 var(--space-4); border-radius: 14px; color: var(--color-text-secondary); font-size: 0.98rem; }
.client-nav a span span { display: inline-grid; place-items: center; width: 28px; color: var(--color-primary); }
.client-nav a.router-link-active, .client-nav a[aria-current="page"] { background: rgba(255,255,255,0.78); color: var(--color-primary); box-shadow: inset 0 0 0 1px rgba(229,219,204,0.72), 0 8px 22px rgba(29,43,31,0.05); }
.nav-badge { display: inline-grid; place-items: center; min-width: 1.45rem; height: 1.45rem; padding: 0 var(--space-1); border-radius: var(--radius-round); background: var(--color-primary); color: #fff; font-size: 0.75rem; font-weight: 900; }
.sidebar-illustration { margin-top: auto; min-height: 11rem; display: grid; place-items: center; color: rgba(29,43,31,0.18); font-size: 4rem; line-height: 1.1; border: 1px dashed rgba(29,43,31,0.14); border-radius: 28px; background: linear-gradient(135deg, rgba(255,255,255,0.72), rgba(240,232,220,0.68)); }
.mobile-bottom-nav { position: fixed; z-index: 30; left: var(--space-3); right: var(--space-3); bottom: calc(var(--space-3) + env(safe-area-inset-bottom)); display: grid; grid-template-columns: repeat(3, 1fr); gap: var(--space-2); padding: var(--space-2); border: 1px solid rgba(229,219,204,0.9); border-radius: 22px; background: rgba(255,255,255,0.96); box-shadow: var(--shadow-navigation); }
.mobile-bottom-nav a { display: flex; align-items: center; justify-content: center; gap: var(--space-1); min-height: 44px; border-radius: var(--radius-medium); color: var(--color-text-secondary); font-size: 0.88rem; }
.mobile-bottom-nav a.router-link-active, .mobile-bottom-nav a[aria-current="page"] { background: var(--color-primary); color: #fff; }
@media (min-width: 960px) {
  .client-shell { display: grid; grid-template-columns: 268px minmax(0, 1fr); }
  .client-sidebar { position: sticky; top: 0; min-height: 100vh; display: flex; flex-direction: column; gap: var(--space-10); padding: 38px 24px 28px; background: linear-gradient(180deg, rgba(251,248,242,0.96), rgba(240,232,220,0.92)); border-right: 1px solid rgba(229,219,204,0.82); box-shadow: 10px 0 34px rgba(29,43,31,0.04); }
  .mobile-client-header, .mobile-bottom-nav { display: none; }
  .client-page-shell { padding: 52px clamp(34px, 4vw, 56px); width: min(1260px, 100%); margin: 0 auto; }
}
</style>
