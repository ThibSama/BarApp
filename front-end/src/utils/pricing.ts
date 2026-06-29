import type { CartItem, Cocktail, OrderItem } from '@/types/domain';

export function calculateCartTotal(items: CartItem[], cocktails: Cocktail[]): number {
  return items.reduce((total, item) => {
    const cocktail = cocktails.find((entry) => entry.id === item.cocktailId);
    return total + (cocktail?.prices[item.size] ?? 0) * item.quantity;
  }, 0);
}

export function calculateOrderTotal(items: OrderItem[]): number {
  return items.reduce((total, item) => total + item.unitPrice * item.quantity, 0);
}
