import { describe, expect, it } from 'vitest';
import { getPaymentMethodLabel, getPaymentSimulationMessage } from '@/utils/payments';

describe('payment utilities', () => {
  it('returns French payment labels', () => {
    expect(getPaymentMethodLabel('cash_at_counter')).toBe('Espèces au comptoir');
    expect(getPaymentMethodLabel('card_at_counter')).toBe('Carte bancaire au comptoir');
    expect(getPaymentMethodLabel('card_in_app')).toBe('Carte bancaire dans l’application');
    expect(getPaymentMethodLabel('apple_pay')).toBe('Apple Pay');
    expect(getPaymentMethodLabel('google_pay')).toBe('Google Pay');
  });

  it('returns the correct simulated payment message', () => {
    expect(getPaymentSimulationMessage('cash_at_counter')).toBe('Le règlement sera effectué au comptoir.');
    expect(getPaymentSimulationMessage('apple_pay')).toBe('Paiement simulé avec succès.');
  });
});
