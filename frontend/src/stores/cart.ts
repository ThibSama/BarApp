import { defineStore } from 'pinia';
import { computed, ref, watch } from 'vue';
import type { ApiSize, CreateOrderItemPayload } from '@/types/api';
import { CART_SCHEMA_VERSION, type CartLine, type PersistedCart } from '@/types/cart';
import { calculateCartTotal } from '@/utils/pricing';
import { SIZE_ORDER } from '@/utils/menu';

const CART_KEY = 'barapp.cart.v2';

/** Input snapshot used when adding to the cart (resolved from the live menu). */
export interface AddCartItemInput {
  cocktailId: number;
  name: string;
  size: ApiSize;
  unitPrice: number;
  imageUrl: string | null;
}

function isValidLine(value: unknown): value is CartLine {
  if (!value || typeof value !== 'object') return false;
  const line = value as Record<string, unknown>;
  return (
    typeof line.id === 'string' &&
    typeof line.cocktailId === 'number' &&
    Number.isInteger(line.cocktailId) &&
    line.cocktailId > 0 &&
    typeof line.nameSnapshot === 'string' &&
    typeof line.size === 'string' &&
    SIZE_ORDER.includes(line.size as ApiSize) &&
    typeof line.unitPriceSnapshot === 'number' &&
    Number.isFinite(line.unitPriceSnapshot) &&
    typeof line.quantity === 'number' &&
    Number.isInteger(line.quantity) &&
    line.quantity > 0 &&
    (line.imageUrlSnapshot === null || typeof line.imageUrlSnapshot === 'string')
  );
}

/**
 * Load the persisted cart, validating each line. Incompatible data (a previous
 * mock cart with slug ids, an unknown version, malformed JSON) is discarded
 * rather than crashing; only well-formed, current-version lines survive.
 */
function loadPersistedCart(): CartLine[] {
  if (typeof localStorage === 'undefined') return [];
  const raw = localStorage.getItem(CART_KEY);
  if (!raw) return [];
  try {
    const parsed = JSON.parse(raw) as PersistedCart;
    if (!parsed || parsed.version !== CART_SCHEMA_VERSION || !Array.isArray(parsed.lines)) {
      return [];
    }
    return parsed.lines.filter(isValidLine);
  } catch {
    return [];
  }
}

function persist(lines: CartLine[]): void {
  if (typeof localStorage === 'undefined') return;
  const envelope: PersistedCart = { version: CART_SCHEMA_VERSION, lines };
  localStorage.setItem(CART_KEY, JSON.stringify(envelope));
}

let lineCounter = 0;
function nextLineId(cocktailId: number, size: ApiSize): string {
  lineCounter += 1;
  return `${cocktailId}-${size}-${lineCounter}`;
}

export const useCartStore = defineStore('cart', () => {
  const items = ref<CartLine[]>(loadPersistedCart());
  watch(items, (value) => persist(value), { deep: true });

  const itemCount = computed(() => items.value.reduce((total, line) => total + line.quantity, 0));
  const total = computed(() => calculateCartTotal(items.value));
  const isEmpty = computed(() => items.value.length === 0);

  /** Add a drink: same cocktail + same size increments; a new size is a new line. */
  function addItem(input: AddCartItemInput, quantity = 1): void {
    if (quantity <= 0) return;
    const existing = items.value.find(
      (line) => line.cocktailId === input.cocktailId && line.size === input.size,
    );
    if (existing) {
      existing.quantity += quantity;
      // Refresh display snapshots so the cart reflects the latest menu data.
      existing.nameSnapshot = input.name;
      existing.unitPriceSnapshot = input.unitPrice;
      existing.imageUrlSnapshot = input.imageUrl;
      return;
    }
    items.value.push({
      id: nextLineId(input.cocktailId, input.size),
      cocktailId: input.cocktailId,
      nameSnapshot: input.name,
      size: input.size,
      unitPriceSnapshot: input.unitPrice,
      quantity,
      imageUrlSnapshot: input.imageUrl,
    });
  }

  function updateQuantity(id: string, quantity: number): void {
    if (quantity <= 0) {
      removeItem(id);
      return;
    }
    const line = items.value.find((entry) => entry.id === id);
    if (line) line.quantity = Math.floor(quantity);
  }

  function removeItem(id: string): void {
    items.value = items.value.filter((line) => line.id !== id);
  }

  function clearCart(): void {
    items.value = [];
  }

  /**
   * Expand the cart into the backend request shape: one entry per physical drink
   * (a line with quantity 3 yields three identical items), matching
   * `CreateOrderItemRequest`.
   */
  function toOrderItems(): CreateOrderItemPayload[] {
    return items.value.flatMap((line) =>
      Array.from({ length: line.quantity }, () => ({
        cocktailId: line.cocktailId,
        size: line.size,
      })),
    );
  }

  return {
    items,
    itemCount,
    total,
    isEmpty,
    addItem,
    updateQuantity,
    removeItem,
    clearCart,
    toOrderItems,
  };
});
