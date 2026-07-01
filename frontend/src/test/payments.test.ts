import { describe, expect, it } from 'vitest';
import { getPaymentMethodLabel, getPaymentSimulationMessage, paymentMethodOptions } from '@/utils/payments';

describe('payment utilities', () => {
  it('returns French labels for the real backend payment enums', () => {
    expect(getPaymentMethodLabel('CASH_AT_COUNTER')).toBe('Espèces au comptoir');
    expect(getPaymentMethodLabel('CARD_AT_COUNTER')).toBe('Carte bancaire au comptoir');
    expect(getPaymentMethodLabel('CARD_IN_APP')).toBe('Carte bancaire dans l’application');
    expect(getPaymentMethodLabel('APPLE_PAY')).toBe('Apple Pay');
    expect(getPaymentMethodLabel('GOOGLE_PAY')).toBe('Google Pay');
  });

  it('exposes exactly the five backend payment-method ids', () => {
    expect(paymentMethodOptions.map((option) => option.id)).toEqual([
      'CASH_AT_COUNTER',
      'CARD_AT_COUNTER',
      'CARD_IN_APP',
      'APPLE_PAY',
      'GOOGLE_PAY',
    ]);
  });

  it('returns the correct simulated payment message', () => {
    expect(getPaymentSimulationMessage('CASH_AT_COUNTER')).toBe('Le règlement sera effectué au comptoir.');
    expect(getPaymentSimulationMessage('APPLE_PAY')).toBe('Paiement simulé avec succès.');
  });
});
