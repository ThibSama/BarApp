import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest, configureApiAuth } from '@/services/apiClient';

function fakeResponse(status: number, body?: unknown) {
  return {
    status,
    ok: status >= 200 && status < 300,
    text: async () => (body === undefined ? '' : JSON.stringify(body)),
  } as Response;
}

describe('apiClient', () => {
  beforeEach(() => {
    // Reset the global auth hooks to defaults before each case.
    configureApiAuth({ getToken: () => null, onUnauthorized: () => {} });
  });

  it('parses a successful JSON response and prefixes /api', async () => {
    const fetchMock = vi.fn().mockResolvedValue(fakeResponse(200, { hello: 'world' }));
    vi.stubGlobal('fetch', fetchMock);

    const data = await apiRequest<{ hello: string }>('/menu');

    expect(data).toEqual({ hello: 'world' });
    expect(fetchMock).toHaveBeenCalledWith('/api/menu', expect.objectContaining({ method: 'GET' }));
  });

  it('returns undefined for 204 No Content', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fakeResponse(204)));
    await expect(apiRequest('/noop', { method: 'PATCH' })).resolves.toBeUndefined();
  });

  it('throws an ApiError carrying status and backend code', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(fakeResponse(404, { code: 'ORDER_NOT_FOUND', message: 'Introuvable', status: 404 })),
    );

    let err: ApiError | undefined;
    try {
      await apiRequest('/bar/orders/x', { auth: true });
    } catch (e) {
      err = e as ApiError;
    }
    expect(err).toBeInstanceOf(ApiError);
    expect(err?.status).toBe(404);
    expect(err?.code).toBe('ORDER_NOT_FOUND');
  });

  it('flags a transport failure as a network error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));

    let err: ApiError | undefined;
    try {
      await apiRequest('/menu');
    } catch (e) {
      err = e as ApiError;
    }
    expect(err).toBeInstanceOf(ApiError);
    expect(err?.isNetworkError).toBe(true);
    expect(err?.status).toBe(0);
  });

  it('adds a Bearer header for authenticated calls and never puts the token in the URL', async () => {
    configureApiAuth({ getToken: () => 'jwt-secret-123', onUnauthorized: () => {} });
    const fetchMock = vi.fn().mockResolvedValue(fakeResponse(200, []));
    vi.stubGlobal('fetch', fetchMock);

    await apiRequest('/bar/orders?completed=false', { auth: true });

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/bar/orders?completed=false');
    expect(url).not.toContain('jwt-secret-123');
    expect((init.headers as Record<string, string>).Authorization).toBe('Bearer jwt-secret-123');
  });

  it('does not attach Authorization for unauthenticated calls', async () => {
    configureApiAuth({ getToken: () => 'jwt-secret-123', onUnauthorized: () => {} });
    const fetchMock = vi.fn().mockResolvedValue(fakeResponse(200, {}));
    vi.stubGlobal('fetch', fetchMock);

    await apiRequest('/auth/login', { method: 'POST', body: { username: 'x', password: 'y' } });

    const [, init] = fetchMock.mock.calls[0];
    expect((init.headers as Record<string, string>).Authorization).toBeUndefined();
  });

  it('invokes the unauthorized handler on a 401 for authenticated calls', async () => {
    const onUnauthorized = vi.fn();
    configureApiAuth({ getToken: () => 'jwt', onUnauthorized });
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(fakeResponse(401, { code: 'INVALID_TOKEN' })));

    await apiRequest('/auth/me', { auth: true }).catch(() => {});
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });
});
