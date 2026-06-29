import type { OrderStatus, PreparationStep, Size } from '@/types/domain';

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

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR' }).format(value);
}

export function formatTime(value: string): string {
  return new Intl.DateTimeFormat('fr-FR', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' }).format(new Date(value));
}
