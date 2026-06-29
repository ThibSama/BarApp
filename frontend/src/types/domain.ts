export type Size = 'S' | 'M' | 'L';
export type OrderStatus = 'ordered' | 'preparing' | 'completed';
export type PreparationStep = 'ingredients' | 'assembly' | 'garnish' | 'completed';
export type PaymentMethod = 'cash_at_counter' | 'card_at_counter' | 'card_in_app' | 'apple_pay' | 'google_pay';

export interface Category {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
}

export interface CocktailPrices {
  S: number;
  M: number;
  L: number;
}

export interface Cocktail {
  id: string;
  name: string;
  description: string;
  shortDescription: string;
  categoryId: string;
  imageUrl: string;
  ingredients: string[];
  prices: CocktailPrices;
  available: boolean;
}

export interface CartItem {
  id: string;
  cocktailId: string;
  size: Size;
  quantity: number;
}

export interface OrderItem {
  id: string;
  cocktailId: string;
  cocktailName: string;
  size: Size;
  quantity: number;
  unitPrice: number;
  preparationStep: PreparationStep;
}

export interface Order {
  id: string;
  orderNumber: string;
  createdAt: string;
  status: OrderStatus;
  paymentMethod: PaymentMethod;
  items: OrderItem[];
  total: number;
}

export interface CocktailFormData {
  name: string;
  description: string;
  shortDescription: string;
  categoryId: string;
  imageUrl: string;
  ingredients: string[];
  prices: CocktailPrices;
  available: boolean;
}
