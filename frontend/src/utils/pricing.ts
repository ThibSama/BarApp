// Cart pricing. The cart total is purely a display figure computed from the
// per-line price snapshots; the backend stays authoritative when the order is
// persisted.
import type { CartLine } from '@/types/cart';

/** Display total of the cart from each line's price snapshot × quantity. */
export function calculateCartTotal(lines: CartLine[]): number {
  return lines.reduce((total, line) => total + line.unitPriceSnapshot * line.quantity, 0);
}
