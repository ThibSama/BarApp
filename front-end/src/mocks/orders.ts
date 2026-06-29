import type { Order } from '@/types/domain';

export const mockOrders: Order[] = [
  { id: 'order-1', orderNumber: 'BA-1024', createdAt: new Date(Date.now() - 1000 * 60 * 12).toISOString(), status: 'ordered', paymentMethod: 'cash_at_counter', total: 29, items: [
    { id: 'oi-1', cocktailId: 'mojito', cocktailName: 'Mojito', size: 'M', quantity: 2, unitPrice: 8.5, preparationStep: 'ingredients' },
    { id: 'oi-2', cocktailId: 'jardin-vert', cocktailName: 'Jardin Vert', size: 'L', quantity: 1, unitPrice: 8.5, preparationStep: 'ingredients' },
    { id: 'oi-3', cocktailId: 'virgin-mojito', cocktailName: 'Virgin Mojito', size: 'S', quantity: 1, unitPrice: 5, preparationStep: 'ingredients' },
  ]},
  { id: 'order-2', orderNumber: 'BA-1025', createdAt: new Date(Date.now() - 1000 * 60 * 28).toISOString(), status: 'preparing', paymentMethod: 'card_at_counter', total: 31.5, items: [
    { id: 'oi-4', cocktailId: 'eclipse-tropicale', cocktailName: 'Éclipse Tropicale', size: 'M', quantity: 1, unitPrice: 10.5, preparationStep: 'assembly' },
    { id: 'oi-5', cocktailId: 'pina-colada', cocktailName: 'Piña Colada', size: 'M', quantity: 1, unitPrice: 9.5, preparationStep: 'garnish' },
    { id: 'oi-6', cocktailId: 'spritz-abricot', cocktailName: 'Spritz Abricot', size: 'L', quantity: 1, unitPrice: 10.5, preparationStep: 'ingredients' },
  ]},
  { id: 'order-3', orderNumber: 'BA-1026', createdAt: new Date(Date.now() - 1000 * 60 * 45).toISOString(), status: 'completed', paymentMethod: 'card_in_app', total: 20, items: [
    { id: 'oi-7', cocktailId: 'moscow-mule', cocktailName: 'Moscow Mule', size: 'M', quantity: 1, unitPrice: 9, preparationStep: 'completed' },
    { id: 'oi-8', cocktailId: 'negroni-doux', cocktailName: 'Negroni Doux', size: 'M', quantity: 1, unitPrice: 10, preparationStep: 'completed' },
    { id: 'oi-9', cocktailId: 'virgin-mojito', cocktailName: 'Virgin Mojito', size: 'S', quantity: 1, unitPrice: 5, preparationStep: 'completed' },
  ]},
  { id: 'order-4', orderNumber: 'BA-1027', createdAt: new Date(Date.now() - 1000 * 60 * 4).toISOString(), status: 'preparing', paymentMethod: 'apple_pay', total: 24.5, items: [
    { id: 'oi-10', cocktailId: 'lune-rose', cocktailName: 'Lune Rose', size: 'M', quantity: 1, unitPrice: 11, preparationStep: 'garnish' },
    { id: 'oi-11', cocktailId: 'jardin-vert', cocktailName: 'Jardin Vert', size: 'M', quantity: 1, unitPrice: 7, preparationStep: 'assembly' },
    { id: 'oi-12', cocktailId: 'mojito', cocktailName: 'Mojito', size: 'S', quantity: 1, unitPrice: 6.5, preparationStep: 'ingredients' },
  ]},
];
