// Controlled French error messages for the barmaker catalogue admin stores.
// Never exposes raw backend traces, SQL or stack details.
import { ApiError } from '@/services/apiClient';

export function describeAdminError(err: unknown): string {
  if (err instanceof ApiError) {
    if (err.isNetworkError) return 'Le serveur est injoignable. Réessayez.';
    switch (err.code) {
      case 'CATEGORY_ALREADY_EXISTS':
        return 'Une catégorie portant ce nom existe déjà.';
      case 'COCKTAIL_ALREADY_EXISTS':
        return 'Un cocktail portant ce nom existe déjà dans cette catégorie.';
      case 'INGREDIENT_ALREADY_EXISTS':
        return 'Un ingrédient portant ce nom existe déjà.';
      case 'USERNAME_ALREADY_EXISTS':
        return 'Ce nom d’utilisateur est déjà utilisé.';
      case 'CATEGORY_INACTIVE':
        return 'Impossible de rattacher un cocktail à une catégorie inactive.';
      case 'CATEGORY_NOT_FOUND':
        return 'Cette catégorie est introuvable.';
      case 'COCKTAIL_NOT_FOUND':
        return 'Ce cocktail est introuvable.';
      case 'INGREDIENT_NOT_FOUND':
        return 'Cet ingrédient est introuvable.';
      case 'VALIDATION_ERROR':
      case 'INVALID_CATALOG_REQUEST':
        return 'Certains champs sont invalides. Vérifiez le formulaire.';
      default:
        if (err.status === 401) return 'Session expirée. Reconnectez-vous.';
        return err.message || 'Une erreur est survenue. Réessayez.';
    }
  }
  return 'Une erreur est survenue. Réessayez.';
}

/** Whether the error is a duplicate-name conflict (HTTP 409). */
export function isConflict(err: unknown): boolean {
  return err instanceof ApiError && err.status === 409;
}
