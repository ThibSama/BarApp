// Protected barmaker catalogue administration API. Every request is
// authenticated; the generic client attaches the JWT and handles 401 centrally.
import { apiRequest } from './apiClient';
import type {
  CategoryRequest,
  CategoryResponse,
  CocktailRequest,
  CocktailResponse,
  IngredientRequest,
  IngredientResponse,
} from '@/types/api';

// --- Categories -----------------------------------------------------------

export function fetchCategories(signal?: AbortSignal): Promise<CategoryResponse[]> {
  return apiRequest<CategoryResponse[]>('/bar/categories', { auth: true, signal });
}

export function createCategory(body: CategoryRequest): Promise<CategoryResponse> {
  return apiRequest<CategoryResponse>('/bar/categories', { method: 'POST', auth: true, body });
}

export function updateCategory(id: number, body: CategoryRequest): Promise<CategoryResponse> {
  return apiRequest<CategoryResponse>(`/bar/categories/${id}`, { method: 'PUT', auth: true, body });
}

/** Logical deactivation (`DELETE` returns 204 No Content). */
export function deleteCategory(id: number): Promise<void> {
  return apiRequest<void>(`/bar/categories/${id}`, { method: 'DELETE', auth: true });
}

// --- Cocktails ------------------------------------------------------------

export function fetchCocktails(signal?: AbortSignal): Promise<CocktailResponse[]> {
  return apiRequest<CocktailResponse[]>('/bar/cocktails', { auth: true, signal });
}

export function fetchCocktail(id: number, signal?: AbortSignal): Promise<CocktailResponse> {
  return apiRequest<CocktailResponse>(`/bar/cocktails/${id}`, { auth: true, signal });
}

export function createCocktail(body: CocktailRequest): Promise<CocktailResponse> {
  return apiRequest<CocktailResponse>('/bar/cocktails', { method: 'POST', auth: true, body });
}

export function updateCocktail(id: number, body: CocktailRequest): Promise<CocktailResponse> {
  return apiRequest<CocktailResponse>(`/bar/cocktails/${id}`, { method: 'PUT', auth: true, body });
}

/** Logical deactivation (`DELETE` returns 204 No Content). */
export function deleteCocktail(id: number): Promise<void> {
  return apiRequest<void>(`/bar/cocktails/${id}`, { method: 'DELETE', auth: true });
}

// --- Ingredients ----------------------------------------------------------

export function fetchIngredients(signal?: AbortSignal): Promise<IngredientResponse[]> {
  return apiRequest<IngredientResponse[]>('/bar/ingredients', { auth: true, signal });
}

export function createIngredient(body: IngredientRequest): Promise<IngredientResponse> {
  return apiRequest<IngredientResponse>('/bar/ingredients', { method: 'POST', auth: true, body });
}

export function updateIngredient(id: number, body: IngredientRequest): Promise<IngredientResponse> {
  return apiRequest<IngredientResponse>(`/bar/ingredients/${id}`, { method: 'PUT', auth: true, body });
}
