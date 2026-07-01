// Payment-method presentation. The IDs are the real backend `PaymentMethod`
// enum names sent verbatim in the order request; only the labels are French
// display text. No real provider, no card/bank data is ever collected.
import type { ApiPaymentMethod } from '@/types/api';

export interface PaymentMethodOption {
  id: ApiPaymentMethod;
  label: string;
  group: 'counter' | 'application';
  icon: string;
}

export const paymentGroupLabels: Record<PaymentMethodOption['group'], string> = {
  counter: 'Paiement au comptoir',
  application: 'Paiement dans l’application',
};

export const paymentMethodLabels: Record<ApiPaymentMethod, string> = {
  CASH_AT_COUNTER: 'Espèces au comptoir',
  CARD_AT_COUNTER: 'Carte bancaire au comptoir',
  CARD_IN_APP: 'Carte bancaire dans l’application',
  APPLE_PAY: 'Apple Pay',
  GOOGLE_PAY: 'Google Pay',
};

export const paymentMethodOptions: PaymentMethodOption[] = [
  { id: 'CASH_AT_COUNTER', label: paymentMethodLabels.CASH_AT_COUNTER, group: 'counter', icon: '💶' },
  { id: 'CARD_AT_COUNTER', label: paymentMethodLabels.CARD_AT_COUNTER, group: 'counter', icon: '💳' },
  { id: 'CARD_IN_APP', label: paymentMethodLabels.CARD_IN_APP, group: 'application', icon: '💳' },
  { id: 'APPLE_PAY', label: paymentMethodLabels.APPLE_PAY, group: 'application', icon: 'Pay' },
  { id: 'GOOGLE_PAY', label: paymentMethodLabels.GOOGLE_PAY, group: 'application', icon: 'G Pay' },
];

/** Whether a payment method is a counter (in-person) settlement. */
export function isCounterPayment(method: ApiPaymentMethod): boolean {
  return method === 'CASH_AT_COUNTER' || method === 'CARD_AT_COUNTER';
}

export function getPaymentMethodLabel(method: ApiPaymentMethod): string {
  return paymentMethodLabels[method];
}

export function getPaymentSimulationMessage(method: ApiPaymentMethod): string {
  return isCounterPayment(method)
    ? 'Le règlement sera effectué au comptoir.'
    : 'Paiement simulé avec succès.';
}
