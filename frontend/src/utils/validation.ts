// Small, domain-free validation helpers reused by the catalogue admin forms and
// the customer checkout. The backend remains the final authority on every rule.

export function validateRequired(value: string, fieldLabel: string): string {
  return value.trim().length > 0 ? '' : `${fieldLabel} est obligatoire.`;
}

export function validatePositivePrice(value: number, fieldLabel: string): string {
  return Number.isFinite(value) && value > 0 ? '' : `${fieldLabel} doit être supérieur à 0.`;
}

/**
 * Validate a table number against the backend rule (integer, 1..25). Returns an
 * empty string when valid, otherwise a French error message.
 */
export function validateTableNumber(value: number | null): string {
  if (value === null || !Number.isFinite(value)) {
    return 'Veuillez saisir votre numéro de table';
  }
  if (!Number.isInteger(value) || value < 1 || value > 25) {
    return 'Le numéro de table doit être compris entre 1 et 25.';
  }
  return '';
}
