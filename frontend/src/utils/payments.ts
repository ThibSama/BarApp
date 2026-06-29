import type { PaymentMethod } from '@/types/domain';

export interface PaymentMethodOption {
  id: PaymentMethod;
  label: string;
  group: 'counter' | 'application';
  icon: string;
}

export const paymentGroupLabels: Record<PaymentMethodOption['group'], string> = {
  counter: 'Paiement au comptoir',
  application: 'Paiement dans l’application',
};

export const paymentMethodLabels: Record<PaymentMethod, string> = {
  cash_at_counter: 'Espèces au comptoir',
  card_at_counter: 'Carte bancaire au comptoir',
  card_in_app: 'Carte bancaire dans l’application',
  apple_pay: 'Apple Pay',
  google_pay: 'Google Pay',
};

export const paymentMethodOptions: PaymentMethodOption[] = [
  { id: 'cash_at_counter', label: paymentMethodLabels.cash_at_counter, group: 'counter', icon: '💶' },
  { id: 'card_at_counter', label: paymentMethodLabels.card_at_counter, group: 'counter', icon: '💳' },
  { id: 'card_in_app', label: paymentMethodLabels.card_in_app, group: 'application', icon: '💳' },
  { id: 'apple_pay', label: paymentMethodLabels.apple_pay, group: 'application', icon: 'Pay' },
  { id: 'google_pay', label: paymentMethodLabels.google_pay, group: 'application', icon: 'G Pay' },
];

export function getPaymentMethodLabel(method: PaymentMethod): string {
  return paymentMethodLabels[method];
}

export function getPaymentSimulationMessage(method: PaymentMethod): string {
  return method === 'cash_at_counter' || method === 'card_at_counter'
    ? 'Le règlement sera effectué au comptoir.'
    : 'Paiement simulé avec succès.';
}
