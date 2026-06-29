import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import { loadState, saveState } from '@/services/localPersistence';
import { useCatalogStore } from './catalog';
import type { CartItem, Cocktail, Size } from '@/types/domain';
import { calculateCartTotal } from '@/utils/pricing';

export interface DetailedCartItem extends CartItem {
  cocktail?: Cocktail;
  unitPrice: number;
  lineTotal: number;
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>(loadState<CartItem[]>('barapp.cart', []));
  watch(items, (value) => saveState('barapp.cart', value), { deep: true });

  const itemCount = computed(() => items.value.reduce((total, item) => total + item.quantity, 0));
  const detailedItems = computed<DetailedCartItem[]>(() => {
    const catalog = useCatalogStore();
    return items.value.map((item) => {
      const cocktail = catalog.getCocktailById(item.cocktailId);
      const unitPrice = cocktail?.prices[item.size] ?? 0;
      return { ...item, cocktail, unitPrice, lineTotal: unitPrice * item.quantity };
    });
  });
  const total = computed(() => {
    const catalog = useCatalogStore();
    return calculateCartTotal(items.value, catalog.cocktails);
  });

  function addItem(cocktailId: string, size: Size, quantity: number): void {
    const existing = items.value.find((item) => item.cocktailId === cocktailId && item.size === size);
    if (existing) existing.quantity += quantity;
    else items.value.push({ id: `${cocktailId}-${size}-${Date.now()}`, cocktailId, size, quantity });
  }

  function updateQuantity(id: string, quantity: number): void {
    if (quantity <= 0) removeItem(id);
    else {
      const item = items.value.find((entry) => entry.id === id);
      if (item) item.quantity = quantity;
    }
  }

  function removeItem(id: string): void {
    items.value = items.value.filter((item) => item.id !== id);
  }

  function clearCart(): void {
    items.value = [];
  }

  return { items, detailedItems, itemCount, total, addItem, updateQuantity, removeItem, clearCart };
});
