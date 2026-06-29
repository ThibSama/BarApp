import type { Category } from '@/types/domain';

export const mockCategories: Category[] = [
  { id: 'classic', name: 'Classiques', description: 'Les références incontournables du bar.', enabled: true },
  { id: 'fruity', name: 'Fruités', description: 'Des recettes fraîches et colorées.', enabled: true },
  { id: 'alcohol-free', name: 'Sans alcool', description: 'Des cocktails travaillés sans alcool.', enabled: true },
  { id: 'signature', name: 'Créations du bar', description: 'Les recettes originales de la maison.', enabled: true },
];
