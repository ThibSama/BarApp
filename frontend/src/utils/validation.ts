import type { CocktailFormData } from '@/types/domain';

export interface ValidationResult {
  valid: boolean;
  errors: Record<string, string>;
}

export function validateRequired(value: string, fieldLabel: string): string {
  return value.trim().length > 0 ? '' : `${fieldLabel} est obligatoire.`;
}

export function validatePositivePrice(value: number, fieldLabel: string): string {
  return Number.isFinite(value) && value > 0 ? '' : `${fieldLabel} doit être supérieur à 0.`;
}

export function validateCocktailForm(form: CocktailFormData): ValidationResult {
  const errors: Record<string, string> = {};
  const requiredFields: Array<[keyof CocktailFormData, string]> = [
    ['name', 'Nom'],
    ['description', 'Description'],
    ['shortDescription', 'Description courte'],
    ['categoryId', 'Catégorie'],
    ['imageUrl', 'Image'],
  ];

  requiredFields.forEach(([field, label]) => {
    const error = validateRequired(String(form[field]), label);
    if (error) errors[field] = error;
  });

  if (form.ingredients.filter((ingredient) => ingredient.trim()).length === 0) {
    errors.ingredients = 'Au moins un ingrédient est obligatoire.';
  }

  (['S', 'M', 'L'] as const).forEach((size) => {
    const error = validatePositivePrice(form.prices[size], `Prix ${size}`);
    if (error) errors[`price${size}`] = error;
  });

  return { valid: Object.keys(errors).length === 0, errors };
}
