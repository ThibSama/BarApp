import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    // Dev-server only: allow the two role-specific loopback hostnames so
    // http://client.localhost:5173 and http://barmaker.localhost:5173 pass
    // Vite's Host-header check. Kept to an exact list — never a wildcard — so
    // the DNS-rebinding protection stays effective for every other host.
    allowedHosts: ['client.localhost', 'barmaker.localhost'],
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setup.ts',
  },
});
