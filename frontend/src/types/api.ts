// Types facing the real Spring Boot backend. These intentionally mirror the
// backend DTO/JSON shapes exactly (uppercase enums, `publicCode`, `totalAmount`,
// nullable `completedAt`, …) and must NOT be merged into the mock/customer
// `Order` model in `@/types/domain`, which is a separate prototype contract.

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

/** Authenticated barmaker profile, returned by login and `GET /api/auth/me`. */
export interface AuthenticatedUser {
  id: number;
  username: string;
  displayName: string;
  role: 'BARMAKER';
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

/** Full order detail (`GET /api/bar/orders/{orderId}` and `next-step`). */
export interface BarOrderDetail {
  id: string;
  publicCode: string;
  status: ApiOrderStatus;
  totalAmount: number;
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
