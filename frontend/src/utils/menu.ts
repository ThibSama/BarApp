// Helpers over the real menu DTOs: deterministic size ordering and price lookup.
import type { ApiSize, MenuCocktail, MenuPrice } from '@/types/api';

/** Canonical small→large ordering used everywhere sizes are displayed. */
export const SIZE_ORDER: ApiSize[] = ['S', 'M', 'L'];

/** The sizes actually offered by a cocktail, in canonical S→M→L order. */
export function availableSizes(prices: MenuPrice[] | undefined): ApiSize[] {
  const offered = new Set((prices ?? []).map((entry) => entry.size));
  return SIZE_ORDER.filter((size) => offered.has(size));
}

/** The unit price for a given size, or undefined when that size is not offered. */
export function priceForSize(prices: MenuPrice[] | undefined, size: ApiSize): number | undefined {
  return (prices ?? []).find((entry) => entry.size === size)?.price;
}

/** A reasonable default size: the smallest one actually offered (or undefined). */
export function defaultSize(cocktail: Pick<MenuCocktail, 'prices'>): ApiSize | undefined {
  return availableSizes(cocktail.prices)[0];
}
