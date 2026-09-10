import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountListPage } from './AccountListPage';

const mockRememberCustomerId = vi.fn();
const queryState = {
  isLoading: false,
  error: null,
  data: [],
  refetch: vi.fn()
};
const customerQueryState = {
  error: null,
  data: { name: 'Test Customer' },
  refetch: vi.fn()
};
const customerListQueryState = {
  error: null,
  data: []
};
const createAccountMutationState = {
  isPending: false,
  mutateAsync: vi.fn()
};

vi.mock('../hooks/useListCustomerAccounts', () => ({
  useListCustomerAccounts: () => queryState
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({
    isAdmin: false,
    rememberCustomerId: mockRememberCustomerId
  })
}));

vi.mock('@tanstack/react-query', () => ({
  useQuery: ({ queryKey }) => {
    if (queryKey[0] === 'customer') {
      return customerQueryState;
    }

    return customerListQueryState;
  },
  useMutation: () => createAccountMutationState
}));

function renderAccountListPage() {
  return render(
    <MemoryRouter initialEntries={['/customer/44/accounts']}>
      <Routes>
        <Route path="/customer/:customerId/accounts" element={<AccountListPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('AccountListPage', () => {
  beforeEach(() => {
    queryState.isLoading = false;
    queryState.error = null;
    queryState.data = [];
    customerQueryState.error = null;
    customerQueryState.data = { name: 'Test Customer' };
    customerListQueryState.error = null;
    customerListQueryState.data = [];
    createAccountMutationState.isPending = false;
    mockRememberCustomerId.mockReset();
  });

  it('renders the empty state when the customer has no accounts', () => {
    queryState.data = [];

    renderAccountListPage();

    expect(screen.getByText('No active accounts')).toBeInTheDocument();
  });

  it('renders account cards when accounts are returned successfully', () => {
    queryState.data = [
      { accountId: 101, accountType: 'SAVINGS', status: 'ACTIVE', balance: '500.00' }
    ];

    renderAccountListPage();

    expect(screen.getByRole('link', { name: /101/i })).toBeInTheDocument();
    expect(screen.getAllByText('SAVINGS').length).toBeGreaterThan(0);
    expect(screen.getAllByText('$500.00').length).toBeGreaterThan(0);
  });

  it('renders the unavailable state when the account query fails', () => {
    queryState.error = {
      response: {
        status: 404,
        data: {
          code: 'CUSTOMER_NOT_FOUND',
          message: 'Customer not found'
        }
      }
    };

    renderAccountListPage();

    expect(screen.getByText('Accounts unavailable')).toBeInTheDocument();
    expect(screen.getAllByText('This customer may have been deleted or is no longer accessible, so their accounts cannot be shown.').length).toBeGreaterThan(0);
  });
});