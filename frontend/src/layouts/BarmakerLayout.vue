<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppIcon from '@/components/common/AppIcon.vue';
import { useAuthStore } from '@/stores/auth';

const router = useRouter();
const auth = useAuthStore();
const menuOpen = ref(false);
// The "Équipe" (staff management) item is present only for managers — it is
// absent (not merely disabled) for regular barmakers, matching the backend
// authorization that returns 403 on /api/bar/users for a barmaker.
const navItems = computed(() => [
  { name: 'bar-orders', label: 'Commandes', icon: 'clipboard-list' as const },
  { name: 'bar-categories', label: 'Catégories', icon: 'tags' as const },
  { name: 'bar-cocktails', label: 'Cocktails', icon: 'martini' as const },
  ...(auth.isManager
    ? [{ name: 'bar-users', label: 'Équipe', icon: 'users' as const }]
    : []),
]);

async function logout(): Promise<void> {
  menuOpen.value = false;
  auth.logout();
  await router.replace({ name: 'bar-login' });
}
</script>

<template>
  <div class="barmaker-shell">
    <aside class="barmaker-sidebar" aria-label="Navigation barmaker bureau">
      <RouterLink class="barmaker-logo" :to="{ name: 'bar-orders' }" aria-label="Le Bar’App - espace Barmaker">
        <span class="logo-mark"><AppIcon name="martini" :size="28" /></span>
        <strong>LE BAR’APP</strong>
        <small>ESPACE BARMAKER</small>
      </RouterLink>
      <nav class="barmaker-nav" aria-label="Navigation barmaker">
        <RouterLink v-for="item in navItems" :key="item.name" :to="{ name: item.name }">
          <span class="nav-icon"><AppIcon :name="item.icon" :size="20" /></span>
          {{ item.label }}
        </RouterLink>
      </nav>
      <div class="barmaker-account">
        <p v-if="auth.displayName" class="account-name" :title="auth.displayName">{{ auth.displayName }}</p>
        <button class="logout-button" type="button" @click="logout"><AppIcon name="power" :size="18" />Déconnexion</button>
      </div>
    </aside>

    <div class="barmaker-content">
      <header class="barmaker-mobile-header">
        <button type="button" aria-label="Ouvrir la navigation barmaker" @click="menuOpen = true"><AppIcon name="menu" :size="22" /></button>
        <RouterLink :to="{ name: 'bar-orders' }">LE BAR’APP</RouterLink>
        <button type="button" class="mobile-logout" aria-label="Déconnexion" @click="logout"><AppIcon name="power" :size="20" /></button>
      </header>
      <main class="barmaker-page-shell"><RouterView /></main>
    </div>

    <Teleport to="body">
      <div v-if="menuOpen" class="drawer-backdrop" @click.self="menuOpen = false">
        <nav class="barmaker-drawer" aria-label="Navigation barmaker mobile">
          <button type="button" aria-label="Fermer la navigation" @click="menuOpen = false"><AppIcon name="x" :size="22" /></button>
          <RouterLink class="drawer-logo" :to="{ name: 'bar-orders' }" @click="menuOpen = false">
            <span class="logo-mark"><AppIcon name="martini" :size="24" /></span>
            <span><strong>LE BAR’APP</strong><small>ESPACE BARMAKER</small></span>
          </RouterLink>
          <RouterLink v-for="item in navItems" :key="item.name" :to="{ name: item.name }" @click="menuOpen = false">
            <span class="nav-icon"><AppIcon :name="item.icon" :size="20" /></span>
            {{ item.label }}
          </RouterLink>
          <p v-if="auth.displayName" class="account-name drawer-account">{{ auth.displayName }}</p>
          <button class="logout-button" type="button" @click="logout"><AppIcon name="power" :size="20" />Déconnexion</button>
        </nav>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.barmaker-shell { min-height: 100vh; background: transparent; }
.barmaker-sidebar { display: none; }
.barmaker-content { min-width: 0; }
.barmaker-page-shell { width: min(1180px, calc(100% - 2rem)); margin: 0 auto; padding: calc(var(--space-6) + env(safe-area-inset-top)) 0 var(--space-10); }
.barmaker-mobile-header { position: sticky; top: 0; z-index: 20; display: grid; grid-template-columns: 44px 1fr auto; align-items: center; gap: var(--space-3); min-height: calc(60px + env(safe-area-inset-top)); padding: calc(var(--space-3) + env(safe-area-inset-top)) var(--space-4) var(--space-3); border-bottom: 1px solid rgba(229,219,204,0.84); background: rgba(251,248,242,0.94); backdrop-filter: blur(14px); }
.barmaker-mobile-header button { width: 44px; height: 44px; display: grid; place-items: center; border: 1px solid var(--color-border); border-radius: var(--radius-medium); background: #fff; color: var(--color-primary); }
.barmaker-mobile-header a { justify-self: center; letter-spacing: 0.08em; font-weight: 900; }
.barmaker-mobile-header .mobile-logout { width: 44px; height: 44px; display: grid; place-items: center; border: 1px solid var(--color-border); border-radius: var(--radius-medium); background: #fff; color: var(--color-primary); }
.barmaker-account { margin-top: auto; display: grid; gap: var(--space-3); }
.account-name { display: flex; align-items: center; gap: var(--space-2); margin: 0; font-weight: 800; color: var(--color-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.logout-button { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-2); min-height: 48px; padding: 0 var(--space-4); border: 1px solid rgba(229,219,204,0.92); border-radius: 14px; background: #fff; color: var(--color-primary); font-weight: 800; cursor: pointer; }
.logout-button:hover { background: var(--color-panel); }
.drawer-account { margin-top: var(--space-3); }
.barmaker-logo { display: grid; justify-items: start; gap: var(--space-2); color: var(--color-primary); text-decoration: none; }
.logo-mark { width: 54px; height: 54px; display: grid; place-items: center; border: 1px solid rgba(229,219,204,0.9); border-radius: var(--radius-large); background: #fff; color: var(--color-primary); box-shadow: var(--shadow-card-soft); }
.barmaker-logo strong { font-family: var(--font-heading); font-size: 1.38rem; letter-spacing: 0.06em; }
.barmaker-logo small, .drawer-logo small { color: var(--color-text-secondary); font-size: 0.68rem; letter-spacing: 0.15em; font-weight: 800; }
.barmaker-nav { display: grid; gap: var(--space-2); }
.barmaker-nav a, .barmaker-drawer a:not(.drawer-logo) { display: flex; align-items: center; gap: var(--space-3); min-height: 52px; padding: 0 var(--space-4); border-radius: 14px; color: var(--color-text-secondary); font-size: 0.98rem; font-weight: 750; }
.nav-icon { width: 28px; display: inline-grid; place-items: center; color: var(--color-primary); }
.barmaker-nav a.router-link-active, .barmaker-nav a[aria-current="page"], .barmaker-drawer a.router-link-active { background: rgba(255,255,255,0.78); color: var(--color-primary); text-decoration: none; box-shadow: inset 0 0 0 1px rgba(229,219,204,0.72), 0 8px 22px rgba(29,43,31,0.05); }
.drawer-backdrop { position: fixed; inset: 0; z-index: 100; background: rgba(17,17,17,0.28); }
.barmaker-drawer { width: min(320px, 86vw); min-height: 100%; display: grid; align-content: start; gap: var(--space-2); padding: var(--space-5); background: var(--color-panel); box-shadow: 12px 0 40px rgba(0,0,0,0.16); }
.barmaker-drawer > button { justify-self: end; width: 44px; height: 44px; display: grid; place-items: center; border: 1px solid var(--color-border); border-radius: var(--radius-round); background: #fff; color: var(--color-primary); }
.drawer-logo { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-3) 0 var(--space-5); color: var(--color-primary); }
.drawer-logo span:last-child { display: grid; gap: 2px; }
.drawer-logo strong { font-family: var(--font-heading); letter-spacing: 0.06em; }
@media (min-width: 960px) {
  .barmaker-shell { display: grid; grid-template-columns: 268px minmax(0, 1fr); }
  .barmaker-sidebar { position: sticky; top: 0; min-height: 100vh; display: flex; flex-direction: column; gap: var(--space-10); padding: 38px 24px 28px; border-right: 1px solid rgba(229,219,204,0.82); background: linear-gradient(180deg, rgba(251,248,242,0.96), rgba(240,232,220,0.92)); box-shadow: 10px 0 34px rgba(29,43,31,0.04); }
  .barmaker-mobile-header { display: none; }
  .barmaker-page-shell { width: min(1260px, 100%); padding: 52px clamp(34px, 4vw, 56px); }
}
</style>
