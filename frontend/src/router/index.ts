import { createRouter, createWebHistory, type Router } from 'vue-router';
import { buildRoutes } from './routes';
import { useAuthStore } from '@/stores/auth';

/**
 * Build a router whose paths depend on the current hostname while route names
 * stay stable across every mode. The auth guard is identical in all modes and
 * only ever navigates by name, so it produces the correct hostname-relative
 * destination automatically.
 */
export function createAppRouter(hostname: string | null | undefined): Router {
  const router = createRouter({
    history: createWebHistory(),
    routes: buildRoutes(hostname),
  });

  router.beforeEach(async (to) => {
    // Public client routes never touch the auth store (so no Pinia dependency).
    if (to.name !== 'bar-login' && !to.meta.requiresAuth) return true;

    const auth = useAuthStore();

    // Already-authenticated barmaker should not see the login screen.
    if (to.name === 'bar-login') {
      const valid = auth.isAuthenticated || (Boolean(auth.accessToken) && (await auth.ensureSession()));
      return valid ? { name: 'bar-orders' } : true;
    }

    if (to.meta.requiresAuth) {
      const valid = auth.isAuthenticated || (Boolean(auth.accessToken) && (await auth.ensureSession()));
      if (!valid) {
        // Preserve the intended internal destination as a safe redirect query.
        return { name: 'bar-login', query: { redirect: to.fullPath } };
      }
      // Manager-only routes: an authenticated regular barmaker is sent back to
      // the orders queue. The session is already validated above, so isManager
      // reflects the live database role — a barmaker never even briefly renders
      // the manager page on direct URL entry or a browser refresh.
      if (to.meta.requiresManager && !auth.isManager) {
        return { name: 'bar-orders' };
      }
      return true;
    }

    return true;
  });

  return router;
}

// Singleton router for the running app, keyed off the browser hostname. In the
// jsdom test environment this resolves to `localhost` (legacy mode), matching
// the historical routing the existing suite asserts against.
const router = createAppRouter(typeof window !== 'undefined' ? window.location.hostname : 'localhost');

export default router;
