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

  it('enforces the backend table-number rule (1..999 integer)', () => {
    expect(validateTableNumber(null)).toBe('Le numéro de table est obligatoire.');
    expect(validateTableNumber(0)).toBe('Le numéro de table doit être compris entre 1 et 999.');
    expect(validateTableNumber(1000)).toBe('Le numéro de table doit être compris entre 1 et 999.');
    expect(validateTableNumber(12.5)).toBe('Le numéro de table doit être compris entre 1 et 999.');
    expect(validateTableNumber(1)).toBe('');
    expect(validateTableNumber(999)).toBe('');
  });
});
