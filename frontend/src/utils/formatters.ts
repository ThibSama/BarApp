import type { OrderStatus, PreparationStep, Size } from '@/types/domain';
import type { ApiOrderStatus, ApiPreparationStatus } from '@/types/api';

export const sizeLabels: Record<Size, string> = { S: 'S', M: 'M', L: 'L' };

export const orderStatusLabels: Record<OrderStatus, string> = {
  ordered: 'Commandée',
  preparing: 'En cours de préparation',
  completed: 'Terminée',
};

export const preparationStepLabels: Record<PreparationStep, string> = {
  ingredients: 'Préparation des ingrédients',
  assembly: 'Assemblage',
  garnish: 'Dressage',
  completed: 'Terminée',
};

export const preparationSteps: PreparationStep[] = ['ingredients', 'assembly', 'garnish', 'completed'];

// --- Backend (barmaker) enum labels. Kept separate from the mock/customer
// labels above so the two contracts never bleed into each other. ---

export const apiOrderStatusLabels: Record<ApiOrderStatus, string> = {
  ORDERED: 'Commandée',
  IN_PROGRESS: 'En préparation',
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
