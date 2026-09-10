import { beforeEach, describe, expect, it, vi } from 'vitest';

const createMockClient = () => {
  const requestUse = vi.fn();
  const responseUse = vi.fn();
  return {
    interceptors: {
      request: {
        use: requestUse
      },
      response: {
        use: responseUse
      }
    },
    __requestUse: requestUse,
    __responseUse: responseUse
  };
};

describe('axiosClient', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  it('attaches authorization headers from stored auth state', async () => {
    const loginClient = createMockClient();
    const accountClient = createMockClient();
    const create = vi.fn()
      .mockReturnValueOnce(loginClient)
      .mockReturnValueOnce(accountClient);

    vi.doMock('axios', () => ({
      default: {
        create
      }
    }));
    vi.doMock('../auth/authState', () => ({
      readStoredAuthState: () => ({ accessToken: 'secure-token' })
    }));

    const module = await import('./axiosClient');
    const loginInterceptor = loginClient.__requestUse.mock.calls[0][0];
    const accountInterceptor = accountClient.__requestUse.mock.calls[0][0];

    expect(module.loginApiClient).toBe(loginClient);
    expect(module.accountApiClient).toBe(accountClient);
    expect(create).toHaveBeenCalledTimes(2);
    expect(loginInterceptor({ headers: { Existing: 'value' } })).toEqual({
      headers: {
        Existing: 'value',
        Authorization: 'Bearer secure-token'
      }
    });
    expect(accountInterceptor({})).toEqual({
      headers: {
        Authorization: 'Bearer secure-token'
      }
    });
  });

  it('maps backend and validation axios errors into UI-friendly errors', async () => {
    const create = vi.fn()
      .mockReturnValueOnce(createMockClient())
      .mockReturnValueOnce(createMockClient());

    vi.doMock('axios', () => ({
      default: {
        create
      }
    }));
    vi.doMock('../auth/authState', () => ({
      readStoredAuthState: () => ({ accessToken: '' })
    }));

    const { mapAxiosError } = await import('./axiosClient');

    expect(mapAxiosError({
      response: {
        status: 404,
        data: {
          code: 'CUSTOMER_NOT_FOUND',
          message: 'Customer not found',
          field: 'customerId'
        }
      }
    })).toEqual({
      code: 'CUSTOMER_NOT_FOUND',
      message: 'Customer not found',
      field: 'customerId'
    });

    expect(mapAxiosError({
      response: {
        status: 422,
        data: {
          errors: [{ defaultMessage: 'Amount must be positive' }]
        }
      }
    })).toEqual({
      code: 'HTTP_422',
      message: 'Amount must be positive',
      field: null
    });

    expect(mapAxiosError({
      response: {
        status: 500,
        data: 'Server exploded'
      }
    })).toEqual({
      code: 'HTTP_500',
      message: 'Server exploded',
      field: null
    });
  });
});