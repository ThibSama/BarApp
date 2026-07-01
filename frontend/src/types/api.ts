// Types facing the real Spring Boot backend. These intentionally mirror the
// backend DTO/JSON shapes exactly (uppercase enums, `publicCode`, `totalAmount`,
// nullable `completedAt`, …). They are the single contract for every production
// screen; the only frontend-only shape lives in `@/types/cart`.

/** Global order lifecycle (`OrderStatus` on the backend). */
export type ApiOrderStatus = 'ORDERED' | 'IN_PROGRESS' | 'COMPLETED';

/** Per-cocktail preparation lifecycle (`PreparationStatus` on the backend). */
export type ApiPreparationStatus =
  | 'PREPARATION_INGREDIENTS'
  | 'ASSEMBLY'
  | 'DRESSING'
  | 'COMPLETED';

/** Drink size (`CocktailSize` on the backend). */
export type ApiSize = 'S' | 'M' | 'L';

/** Selected payment method (`PaymentMethod` on the backend). Persisted verbatim. */
export type ApiPaymentMethod =
  | 'CASH_AT_COUNTER'
  | 'CARD_AT_COUNTER'
  | 'CARD_IN_APP'
  | 'APPLE_PAY'
  | 'GOOGLE_PAY';

/** Authenticated staff role (`UserRole` on the backend). */
export type UserRole = 'BARMAKER' | 'MANAGER';

/** Authenticated staff profile, returned by login and `GET /api/auth/me`. */
export interface AuthenticatedUser {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
}

/** Successful `POST /api/auth/login` response. */
export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthenticatedUser;
}

/** Compact order summary for the barmaker queue (`GET /api/bar/orders`). */
export interface BarOrderSummary {
  id: string;
  publicCode: string;
  status: ApiOrderStatus;
  totalAmount: number;
  tableNumber: number;
  createdAt: string;
  completedAt: string | null;
  itemCount: number;
  completedItemCount: number;
}

/** One physical cocktail inside a detailed order. */
export interface BarOrderItem {
  id: string;
  sequenceNumber: number;
  cocktailName: string;
  size: ApiSize;
  unitPrice: number;
  preparationStatus: ApiPreparationStatus;
  completedAt: string | null;
}

/** Full order detail (`GET /api/bar/orders/{orderId}` and `next-step`). The
 *  backend serves the same `OrderResponse` here as on the customer side, so the
 *  physical `tableNumber` and selected `paymentMethod` are always present. */
export interface BarOrderDetail {
  id: string;
  publicCode: string;
  status: ApiOrderStatus;
  totalAmount: number;
  tableNumber: number;
  paymentMethod: ApiPaymentMethod;
  createdAt: string;
  completedAt: string | null;
  items: BarOrderItem[];
}

/** A single invalid request field, mirrors backend `FieldErrorDetail`. */
export interface ApiFieldError {
  field: string;
  message: string;
}

/** Backend `ApiErrorResponse` envelope (all fields optional defensively). */
export interface ApiErrorBody {
  timestamp?: string;
  status?: number;
  code?: string;
  message?: string;
  path?: string;
  fieldErrors?: ApiFieldError[];
}

// --- Public customer menu (`GET /api/menu`) -------------------------------

/** One price line (size + amount) on a menu cocktail. */
export interface MenuPrice {
  size: ApiSize;
  price: number;
}

/** One ingredient line on a menu cocktail. */
export interface MenuIngredient {
  id: number;
  name: string;
  quantityLabel: string | null;
  displayOrder: number;
}

/** A single cocktail as exposed on the public menu (active cocktails only). */
export interface MenuCocktail {
  id: number;
  name: string;
  description: string;
  imageUrl: string | null;
  ingredients: MenuIngredient[];
  prices: MenuPrice[];
}

/** A menu category with its (active) cocktails, already ordered server-side. */
export interface MenuCategory {
  id: number;
  name: string;
  displayOrder: number;
  cocktails: MenuCocktail[];
}

/** Root `GET /api/menu` payload. `categories` is never null (empty when bare). */
export interface MenuResponse {
  categories: MenuCategory[];
}

// --- Customer orders (`POST /api/orders`, `GET /api/orders/{id}`) ---------

/** One requested physical drink. The same cocktail/size may repeat (qty>1). */
export interface CreateOrderItemPayload {
  cocktailId: number;
  size: ApiSize;
}

/** Anonymous customer order creation payload. Prices/totals are server-side. */
export interface CreateOrderPayload {
  items: CreateOrderItemPayload[];
  tableNumber: number;
  paymentMethod: ApiPaymentMethod;
}

/** One tracked drink within a customer order (`OrderItemResponse`). */
export interface OrderItem {
  id: string;
  sequenceNumber: number;
  cocktailName: string;
  size: ApiSize;
  unitPrice: number;
  preparationStatus: ApiPreparationStatus;
  completedAt: string | null;
}

/** Stable customer-facing order (`OrderResponse`) for confirmation + tracking. */
export interface CustomerOrder {
  id: string;
  publicCode: string;
  status: ApiOrderStatus;
  totalAmount: number;
  tableNumber: number;
  paymentMethod: ApiPaymentMethod;
  createdAt: string;
  completedAt: string | null;
  items: OrderItem[];
}

// --- Protected catalogue administration (`/api/bar/...`) ------------------

/** A category in the management API (active and inactive alike). */
export interface CategoryResponse {
  id: number;
  name: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
}

/** Create/update payload for a category. */
export interface CategoryRequest {
  name: string;
  description: string | null;
  displayOrder: number;
  active: boolean;
}

/** One price line of a managed cocktail. */
export interface CocktailPriceResponse {
  size: ApiSize;
  price: number;
}

/** One ingredient line of a managed cocktail. */
export interface CocktailIngredientResponse {
  id: number;
  name: string;
  quantityLabel: string | null;
  displayOrder: number;
}

/** A cocktail in the management API (active and inactive alike). */
export interface CocktailResponse {
  id: number;
  categoryId: number;
  categoryName: string;
  name: string;
  description: string;
  imageUrl: string | null;
  active: boolean;
  ingredients: CocktailIngredientResponse[];
  prices: CocktailPriceResponse[];
}

/** One ingredient line of a cocktail management request. */
export interface CocktailIngredientRequest {
  name: string;
  quantityLabel: string | null;
  displayOrder: number;
}

/** One price line of a cocktail management request. */
export interface CocktailPriceRequest {
  size: ApiSize;
  price: number;
}

/** Create/update payload for a cocktail. */
export interface CocktailRequest {
  categoryId: number;
  name: string;
  description: string;
  imageUrl: string | null;
  active: boolean;
  ingredients: CocktailIngredientRequest[];
  prices: CocktailPriceRequest[];
}

// --- Manager-only staff administration (`/api/bar/users`) -----------------

/** A staff account as returned by the manager-only `/api/bar/users` endpoints.
 *  Never carries a password or password hash. */
export interface UserAdminResponse {
  id: number;
  username: string;
  displayName: string;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

/** Manager-only payload to create a new barmaker (`POST /api/bar/users`). The
 *  role is fixed server-side to `BARMAKER`; it is never part of this contract. */
export interface CreateBarmakerRequest {
  displayName: string;
  username: string;
  password: string;
}

/** An ingredient in the management API (active and inactive alike). */
export interface IngredientResponse {
  id: number;
  name: string;
  active: boolean;
}

/** Create/update payload for an ingredient. */
export interface IngredientRequest {
  name: string;
  active: boolean;
}
