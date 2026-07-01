// Test-only fixtures mirroring the real backend DTO shapes. These live under the
// test tree and are NEVER imported by runtime code (which talks only to the API).
import type {
  BarOrderDetail,
  BarOrderSummary,
  CategoryResponse,
  CocktailResponse,
  CustomerOrder,
  IngredientResponse,
  MenuResponse,
} from '@/types/api';

/** A realistic `GET /api/menu` payload with three categories. */
export function menuResponse(): MenuResponse {
  return {
    categories: [
      {
        id: 1,
        name: 'Classiques',
        displayOrder: 1,
        cocktails: [
          {
            id: 101,
            name: 'Mojito',
            description: 'Un grand classique cubain, vif et rafraîchissant.',
            imageUrl: null,
            ingredients: [
              { id: 1, name: 'Rhum blanc', quantityLabel: '4 cl', displayOrder: 0 },
              { id: 2, name: 'Menthe', quantityLabel: null, displayOrder: 1 },
              { id: 3, name: 'Citron vert', quantityLabel: null, displayOrder: 2 },
            ],
            prices: [
              { size: 'S', price: 6.5 },
              { size: 'M', price: 8.5 },
              { size: 'L', price: 10.5 },
            ],
          },
        ],
      },
      {
        id: 2,
        name: 'Fruités',
        displayOrder: 2,
        cocktails: [
          {
            id: 102,
            name: 'Piña Colada',
            description: 'Onctueux et tropical, ananas et coco.',
            imageUrl: null,
            ingredients: [{ id: 4, name: 'Ananas', quantityLabel: null, displayOrder: 0 }],
            prices: [
              { size: 'S', price: 7.5 },
              { size: 'M', price: 9.5 },
              { size: 'L', price: 12 },
            ],
          },
        ],
      },
      {
        id: 3,
        name: 'Sans alcool',
        displayOrder: 3,
        cocktails: [
          {
            id: 103,
            name: 'Jardin Vert',
            description: 'Création végétale et légère.',
            imageUrl: null,
            ingredients: [
              { id: 5, name: 'Concombre', quantityLabel: null, displayOrder: 0 },
              { id: 6, name: 'Basilic', quantityLabel: null, displayOrder: 1 },
            ],
            prices: [
              { size: 'S', price: 5.5 },
              { size: 'M', price: 7 },
              { size: 'L', price: 8.5 },
            ],
          },
        ],
      },
    ],
  };
}

export function categoryResponses(): CategoryResponse[] {
  return [
    { id: 1, name: 'Classiques', description: 'Les références.', displayOrder: 1, active: true },
    { id: 2, name: 'Fruités', description: 'Frais et colorés.', displayOrder: 2, active: true },
    { id: 3, name: 'Sans alcool', description: 'Sans alcool.', displayOrder: 3, active: false },
  ];
}

export function ingredientResponses(): IngredientResponse[] {
  return [
    { id: 1, name: 'Rhum blanc', active: true },
    { id: 2, name: 'Menthe', active: true },
    { id: 7, name: 'Sirop ancien', active: false },
  ];
}

export function cocktailResponse(overrides: Partial<CocktailResponse> = {}): CocktailResponse {
  return {
    id: 101,
    categoryId: 1,
    categoryName: 'Classiques',
    name: 'Mojito',
    description: 'Un grand classique cubain.',
    shortDescription: 'Menthe, citron vert, rhum.',
    imageUrl: null,
    active: true,
    ingredients: [
      { id: 1, name: 'Rhum blanc', quantityLabel: '4 cl', displayOrder: 0 },
      { id: 2, name: 'Menthe', quantityLabel: null, displayOrder: 1 },
    ],
    prices: [
      { size: 'S', price: 6.5 },
      { size: 'M', price: 8.5 },
      { size: 'L', price: 10.5 },
    ],
    ...overrides,
  };
}

export function cocktailResponses(): CocktailResponse[] {
  return [
    cocktailResponse(),
    cocktailResponse({
      id: 102,
      categoryId: 2,
      categoryName: 'Fruités',
      name: 'Piña Colada',
      shortDescription: 'Ananas, coco, rhum.',
      active: false,
    }),
  ];
}

export function customerOrder(overrides: Partial<CustomerOrder> = {}): CustomerOrder {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    publicCode: 'BA-1042',
    status: 'ORDERED',
    totalAmount: 17,
    tableNumber: 12,
    paymentMethod: 'CARD_AT_COUNTER',
    createdAt: '2026-06-30T10:00:00Z',
    completedAt: null,
    items: [
      { id: 'i1', sequenceNumber: 1, cocktailName: 'Mojito', size: 'M', unitPrice: 8.5, preparationStatus: 'PREPARATION_INGREDIENTS', completedAt: null },
      { id: 'i2', sequenceNumber: 2, cocktailName: 'Mojito', size: 'M', unitPrice: 8.5, preparationStatus: 'PREPARATION_INGREDIENTS', completedAt: null },
    ],
    ...overrides,
  };
}

export function barOrderSummary(overrides: Partial<BarOrderSummary> = {}): BarOrderSummary {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    publicCode: 'BA-1042',
    status: 'ORDERED',
    totalAmount: 17,
    tableNumber: 12,
    createdAt: '2026-06-30T10:00:00Z',
    completedAt: null,
    itemCount: 2,
    completedItemCount: 0,
    ...overrides,
  };
}

export function barOrderDetail(overrides: Partial<BarOrderDetail> = {}): BarOrderDetail {
  return {
    id: '11111111-1111-1111-1111-111111111111',
    publicCode: 'BA-1042',
    status: 'ORDERED',
    totalAmount: 17,
    tableNumber: 12,
    paymentMethod: 'CARD_AT_COUNTER',
    createdAt: '2026-06-30T10:00:00Z',
    completedAt: null,
    items: [
      { id: 'i1', sequenceNumber: 1, cocktailName: 'Mojito', size: 'M', unitPrice: 8.5, preparationStatus: 'PREPARATION_INGREDIENTS', completedAt: null },
    ],
    ...overrides,
  };
}
