// Frontend-only cart state. Deliberately separate from the backend API DTOs:
// the cart never holds prices/names the backend would compute. Each line keeps
// display snapshots (name/price/image frozen at add time) plus the two fields
// the order request actually needs: the real cocktail id and the size.
import type { ApiSize } from './api';

/** Schema version of the persisted cart; bumped on any breaking shape change. */
export const CART_SCHEMA_VERSION = 2;

/** A single cart line. `id` is a local, frontend-only line identifier. */
export interface CartLine {
  id: string;
  cocktailId: number;
  /** Display snapshot of the cocktail name at the time it was added. */
  nameSnapshot: string;
  size: ApiSize;
  /** Display snapshot of the unit price for this size (informational only). */
  unitPriceSnapshot: number;
  quantity: number;
  /** Display snapshot of the image URL (may be null → placeholder is used). */
  imageUrlSnapshot: string | null;
}

/** Envelope persisted in localStorage, version-tagged for safe migration. */
export interface PersistedCart {
  version: number;
  lines: CartLine[];
}
