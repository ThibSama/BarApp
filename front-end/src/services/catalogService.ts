import { mockCategories } from '@/mocks/categories';
import { mockCocktails } from '@/mocks/cocktails';
import type { Category, Cocktail } from '@/types/domain';
import { loadState, saveState } from './localPersistence';

const categoriesKey = 'barapp.categories';
const cocktailsKey = 'barapp.cocktails';

export function loadCategories(): Category[] {
  return loadState(categoriesKey, mockCategories);
}

export function saveCategories(categories: Category[]): void {
  saveState(categoriesKey, categories);
}

export function loadCocktails(): Cocktail[] {
  return loadState(cocktailsKey, mockCocktails);
}

export function saveCocktails(cocktails: Cocktail[]): void {
  saveState(cocktailsKey, cocktails);
}
