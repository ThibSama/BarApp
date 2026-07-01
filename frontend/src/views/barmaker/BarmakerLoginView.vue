<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter, type RouteLocationRaw } from 'vue-router';
import AppIcon from '@/components/common/AppIcon.vue';
import { useAuthStore } from '@/stores/auth';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const username = ref('');
const password = ref('');

/**
 * Only accept internal, non protocol-relative redirect targets. Hostname-aware:
 * the login path itself differs per hostname (`/login` vs `/bar/login`), so we
 * resolve it for the active mode and fall back to the named orders route.
 */
function safeRedirect(): RouteLocationRaw {
  const raw = route.query.redirect;
  const target = typeof raw === 'string' ? raw : '';
  const loginPath = router.resolve({ name: 'bar-login' }).path;
  if (target.startsWith('/') && !target.startsWith('//') && target !== loginPath) return target;
  return { name: 'bar-orders' };
}

async function onSubmit(): Promise<void> {
  if (auth.loading) return;
  const ok = await auth.login(username.value.trim(), password.value);
  if (ok) {
    password.value = '';
    await router.replace(safeRedirect());
  }
}
</script>

<template>
  <main class="login-shell">
    <section class="login-card">
      <div class="login-brand">
        <span class="logo-mark"><AppIcon name="martini" :size="28" /></span>
        <strong>LE BAR’APP</strong>
        <small>ESPACE BARMAKER</small>
      </div>

      <form class="login-form" @submit.prevent="onSubmit" novalidate>
        <h1>Connexion</h1>
        <p class="muted">Accédez au tableau des commandes.</p>

        <label for="login-username">Nom d’utilisateur</label>
        <input
          id="login-username"
          v-model="username"
          name="username"
          type="text"
          autocomplete="username"
          required
          :disabled="auth.loading"
        />

        <label for="login-password">Mot de passe</label>
        <input
          id="login-password"
          v-model="password"
          name="password"
          type="password"
          autocomplete="current-password"
          required
          :disabled="auth.loading"
        />

        <p v-if="auth.error" class="alert error" role="alert">{{ auth.error }}</p>

        <button class="button login-submit" type="submit" :disabled="auth.loading">
          {{ auth.loading ? 'Connexion…' : 'Se connecter' }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-shell { min-height: 100vh; display: grid; place-items: center; padding: var(--space-6); }
.login-card { width: min(420px, 100%); display: grid; gap: var(--space-6); padding: clamp(28px, 4vw, 44px); border: 1px solid rgba(229,219,204,0.9); border-radius: 26px; background: #fff; box-shadow: var(--shadow-card); }
.login-brand { display: grid; justify-items: center; gap: var(--space-1); color: var(--color-primary); text-align: center; }
.logo-mark { width: 54px; height: 54px; display: grid; place-items: center; border: 1px solid rgba(229,219,204,0.9); border-radius: var(--radius-large); background: #fff; color: var(--color-primary); box-shadow: var(--shadow-card-soft); }
.login-brand strong { font-family: var(--font-heading); font-size: 1.38rem; letter-spacing: 0.06em; }
.login-brand small { color: var(--color-text-secondary); font-size: 0.68rem; letter-spacing: 0.15em; font-weight: 800; }
.login-form { display: grid; gap: var(--space-3); }
.login-form h1 { margin: 0; }
.login-form .muted { margin: 0 0 var(--space-2); color: var(--color-text-secondary); }
.login-form label { font-weight: 750; color: var(--color-primary); }
.login-form input { min-height: 48px; padding: 0 14px; border: 1px solid rgba(229,219,204,0.92); border-radius: 14px; background: #fff; font-size: 1rem; }
.login-form input:focus-visible { outline: 2px solid var(--color-accent); outline-offset: 1px; }
.login-submit { margin-top: var(--space-3); min-height: 50px; border-radius: 14px; }
</style>
