import { describe, expect, it } from 'vitest';
import { validatePositivePrice, validateRequired, validateTableNumber } from '@/utils/validation';

describe('validation utilities', () => {
  it('validates required values', () => {
    expect(validateRequired('', 'Nom')).toBe('Nom est obligatoire.');
    expect(validateRequired('Mojito', 'Nom')).toBe('');
  });

  it('rejects invalid prices', () => {
    expect(validatePositivePrice(0, 'Prix S')).toBe('Prix S doit être supérieur à 0.');
    expect(validatePositivePrice(6.5, 'Prix S')).toBe('');
  });

  it('enforces the backend table-number rule (1..25 integer)', () => {
    expect(validateTableNumber(null)).toBe('Veuillez saisir votre numéro de table');
    expect(validateTableNumber(0)).toBe('Le numéro de table doit être compris entre 1 et 25.');
    expect(validateTableNumber(26)).toBe('Le numéro de table doit être compris entre 1 et 25.');
    expect(validateTableNumber(12.5)).toBe('Le numéro de table doit être compris entre 1 et 25.');
    expect(validateTableNumber(1)).toBe('');
    expect(validateTableNumber(25)).toBe('');
  });
});
