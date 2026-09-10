import { beforeEach, describe, expect, it, vi } from 'vitest';

const accountApiClient = {
  post: vi.fn(),
  get: vi.fn(),
  put: vi.fn(),
  delete: vi.fn()
};

const loginApiClient = {
  post: vi.fn(),
  get: vi.fn(),
  patch: vi.fn()
};

vi.mock('./axiosClient', () => ({
  accountApiClient,
  loginApiClient
}));

vi.mock('../types', async () => {
  const actual = await vi.importActual('../types/index.js');
  return {
    ...actual,
    createIdempotencyKey: () => 'generated-idempotency-key'
  };
});

describe('account and customer api wrappers', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('builds the correct account create and update payloads', async () => {
    const { createAccount, updateAccount } = await import('./accounts');
    accountApiClient.post.mockResolvedValueOnce({ data: { accountId: 10 } });
    accountApiClient.put.mockResolvedValueOnce({ data: { ok: true } });

    await expect(createAccount({
      customerId: '44',
      accountType: 'SAVINGS',
      balance: '100.00',
      interestRate: '0.0500'
    })).resolves.toEqual({ accountId: 10 });
    expect(accountApiClient.post).toHaveBeenCalledWith('/customers/44/accounts', {
      accountType: 'SAVINGS',
      balance: 100,
      interestRate: 0.05
    });

    await updateAccount({ accountId: '10', interestRate: '' });
    expect(accountApiClient.put).toHaveBeenCalledWith('/accounts/10', {});
  });

  it('adds idempotency headers and nullable description/category for money movements', async () => {
    const { depositToAccount, withdrawFromAccount, transferBetweenAccounts } = await import('./accounts');
    accountApiClient.post
      .mockResolvedValueOnce({ data: { ok: 'deposit' } })
      .mockResolvedValueOnce({ data: { ok: 'withdraw' } })
      .mockResolvedValueOnce({ data: { ok: 'transfer' } });

    await depositToAccount({ accountId: '12', amount: '5.00', description: '', category: '' });
    await withdrawFromAccount({ accountId: '12', amount: '3.00', description: 'ATM', category: '' });
    await transferBetweenAccounts({ fromAccountId: 12, toAccountId: 18, amount: '8.00', description: '', category: 'Food' });

    expect(accountApiClient.post).toHaveBeenNthCalledWith(1,
      '/accounts/12/deposit',
      { amount: '5.00', description: null, category: null },
      { headers: { 'Idempotency-Key': 'generated-idempotency-key' } }
    );
    expect(accountApiClient.post).toHaveBeenNthCalledWith(2,
      '/accounts/12/withdraw',
      { amount: '3.00', description: 'ATM', category: null },
      { headers: { 'Idempotency-Key': 'generated-idempotency-key' } }
    );
    expect(accountApiClient.post).toHaveBeenNthCalledWith(3,
      '/accounts/transfer',
      { fromAccountId: 12, toAccountId: 18, amount: '8.00', description: null, category: 'Food' },
      { headers: { 'Idempotency-Key': 'generated-idempotency-key' } }
    );
  });

  it('calls customer endpoints with the expected payloads', async () => {
    const { createCustomer, getCustomer, listCustomers, updateCustomer } = await import('./customers');
    loginApiClient.post.mockResolvedValueOnce({ data: { customerId: '9' } });
    loginApiClient.get
      .mockResolvedValueOnce({ data: [{ customerId: '9' }] })
      .mockResolvedValueOnce({ data: { customerId: '9', name: 'A. Customer' } });
    loginApiClient.patch.mockResolvedValueOnce({ data: { ok: true } });

    await createCustomer({ name: 'A. Customer' }, 'token-123');
    await listCustomers();
    await getCustomer('9');
    await updateCustomer('9', { address: 'New Street' });

    expect(loginApiClient.post).toHaveBeenCalledWith('/api/customers', { name: 'A. Customer' }, {
      headers: {
        Authorization: 'Bearer token-123'
      }
    });
    expect(loginApiClient.get).toHaveBeenNthCalledWith(1, '/api/customers');
    expect(loginApiClient.get).toHaveBeenNthCalledWith(2, '/api/customers/9');
    expect(loginApiClient.patch).toHaveBeenCalledWith('/api/customers/9', { address: 'New Street' });
  });
});