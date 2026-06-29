export function loadState<T>(key: string, fallback: T): T {
  if (typeof localStorage === 'undefined') return fallback;
  const value = localStorage.getItem(key);
  if (!value) return fallback;
  try {
    return JSON.parse(value) as T;
  } catch {
    return fallback;
  }
}

export function saveState<T>(key: string, value: T): void {
  if (typeof localStorage === 'undefined') return;
  localStorage.setItem(key, JSON.stringify(value));
}
