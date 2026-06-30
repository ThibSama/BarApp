// Centralized, typed HTTP client built on the native `fetch` API.
//
// Design constraints:
//  - relative `/api` URLs only, so the same code works behind the Nginx reverse
//    proxy in Docker and through any dev proxy;
//  - the JWT is injected here for authenticated calls only, never logged, never
//    placed in a query parameter;
//  - to avoid circular imports the client never imports the Pinia store or the
//    router. Instead the auth store registers a token provider and an
//    "unauthorized" handler through `configureApiAuth`.
import type { ApiErrorBody, ApiFieldError } from '@/types/api';

/**
 * Error thrown for any non-2xx HTTP response or transport failure. Carries the
 * HTTP status, the backend machine-readable `code`, and any field errors so
 * callers can branch deterministically without parsing strings.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code?: string;
  readonly fieldErrors?: ApiFieldError[];
  /** True when the request never reached the server (offline, DNS, CORS, …). */
  readonly isNetworkError: boolean;

  constructor(params: {
    message: string;
    status: number;
    code?: string;
    fieldErrors?: ApiFieldError[];
    isNetworkError?: boolean;
  }) {
    super(params.message);
    this.name = 'ApiError';
    this.status = params.status;
    this.code = params.code;
    this.fieldErrors = params.fieldErrors;
    this.isNetworkError = params.isNetworkError ?? false;
  }
}

type TokenProvider = () => string | null;
type UnauthorizedHandler = () => void;

let tokenProvider: TokenProvider = () => null;
let unauthorizedHandler: UnauthorizedHandler = () => {};

/**
 * Wire the client to the authentication layer. Called once by the auth store so
 * that the client can read the current token and react to 401 responses without
 * importing the store directly.
 */
export function configureApiAuth(options: {
  getToken: TokenProvider;
  onUnauthorized: UnauthorizedHandler;
}): void {
  tokenProvider = options.getToken;
  unauthorizedHandler = options.onUnauthorized;
}

export interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE';
  /** Plain object serialized as a JSON body. Omit for body-less requests. */
  body?: unknown;
  /** When true, attach `Authorization: Bearer <token>` and handle 401. */
  auth?: boolean;
  /** Optional AbortSignal to cancel stale requests (used by polling). */
  signal?: AbortSignal;
}

const BASE_PATH = '/api';

async function parseJsonSafely(response: Response): Promise<unknown> {
  const text = await response.text();
  if (!text) return undefined;
  try {
    return JSON.parse(text) as unknown;
  } catch {
    // Non-JSON payload (proxy HTML error page, plain text, …).
    return undefined;
  }
}

/**
 * Perform a typed API request. Resolves with the parsed JSON body (or
 * `undefined` for `204 No Content`); rejects with an {@link ApiError} for any
 * HTTP error or transport failure.
 */
export async function apiRequest<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = 'GET', body, auth = false, signal } = options;
  const headers: Record<string, string> = { Accept: 'application/json' };

  const hasBody = body !== undefined && body !== null;
  if (hasBody) headers['Content-Type'] = 'application/json';

  if (auth) {
    const token = tokenProvider();
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_PATH}${path}`, {
      method,
      headers,
      body: hasBody ? JSON.stringify(body) : undefined,
      signal,
    });
  } catch (error) {
    // Re-throw genuine aborts untouched so callers can ignore them.
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError({
      message: 'Le serveur est injoignable. Vérifiez votre connexion.',
      status: 0,
      isNetworkError: true,
    });
  }

  if (response.status === 204) return undefined as T;

  const payload = await parseJsonSafely(response);

  if (!response.ok) {
    const errorBody = (payload ?? {}) as ApiErrorBody;
    if (response.status === 401 && auth) {
      // Let the auth layer invalidate the local session before we surface it.
      unauthorizedHandler();
    }
    throw new ApiError({
      message: errorBody.message ?? `Erreur ${response.status}`,
      status: response.status,
      code: errorBody.code,
      fieldErrors: errorBody.fieldErrors,
    });
  }

  return payload as T;
}
