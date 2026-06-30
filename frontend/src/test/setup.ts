import { afterEach, vi } from 'vitest';
import { cleanup } from '@testing-library/vue';

afterEach(() => {
  cleanup();
  localStorage.clear();
  sessionStorage.clear();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});
