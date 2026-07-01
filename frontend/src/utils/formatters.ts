import type { ApiOrderStatus, ApiPreparationStatus } from '@/types/api';

// --- Backend enum labels. These are the single source of truth for both the
// barmaker and the customer screens, which now share the real API contract. ---

export const apiOrderStatusLabels: Record<ApiOrderStatus, string> = {
  ORDERED: 'Commandée',
  IN_PROGRESS: 'En cours de préparation',
  COMPLETED: 'Terminée',
};

export const apiPreparationStatusLabels: Record<ApiPreparationStatus, string> = {
  PREPARATION_INGREDIENTS: 'Préparation des ingrédients',
  ASSEMBLY: 'Assemblage',
  DRESSING: 'Dressage',
  COMPLETED: 'Terminée',
};

export const apiPreparationSteps: ApiPreparationStatus[] = [
  'PREPARATION_INGREDIENTS',
  'ASSEMBLY',
  'DRESSING',
  'COMPLETED',
];

export function apiOrderStatusTone(status: ApiOrderStatus): 'neutral' | 'success' | 'warning' {
  if (status === 'COMPLETED') return 'success';
  if (status === 'IN_PROGRESS') return 'warning';
  return 'neutral';
}

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(value);
}

export function formatTime(value: string): string {
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }).format(new Date(value));
}
