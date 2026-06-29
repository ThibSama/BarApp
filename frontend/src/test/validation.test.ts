import { describe, expect, it } from 'vitest';
import { validateCocktailForm, validatePositivePrice, validateRequired } from '@/utils/validation';

describe('validation utilities', () => {
  it('validates required values', () => {
    expect(validateRequired('', 'Nom')).toBe('Nom est obligatoire.');
    expect(validateRequired('Mojito', 'Nom')).toBe('');
  });

  it('rejects invalid prices', () => {
    expect(validatePositivePrice(0, 'Prix S')).toBe('Prix S doit être supérieur à 0.');
  });

  it('validates cocktail form data', () => {
    const result = validateCocktailForm({ name: '', description: '', shortDescription: '', categoryId: '', imageUrl: '', ingredients: [''], prices: { S: 0, M: 7, L: 9 }, available: true });
    expect(result.valid).toBe(false);
    expect(result.errors.ingredients).toBe('Au moins un ingrédient est obligatoire.');
  });
});
