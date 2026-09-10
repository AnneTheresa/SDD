import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountAdminListPage } from '../../pages/AccountAdminListPage';

const allAccountsState = {
  data: [],
  isLoading: false,
  error: null
};

vi.mock('../../hooks/useAllAccounts', () => ({
  useAllAccounts: () => allAccountsState
}));

describe('AccountAdminListPage', () => {
  beforeEach(() => {
    allAccountsState.data = [];
    allAccountsState.error = null;
    allAccountsState.isLoading = false;
  });

  it('shows frozen status in admin list', () => {
    allAccountsState.data = [
      { accountId: 1001, customerId: 42, accountType: 'CHECKING', status: 'FROZEN' }
    ];

    render(
      <MemoryRouter>
        <AccountAdminListPage />
      </MemoryRouter>
    );

    expect(screen.getByText('FROZEN')).toBeInTheDocument();
  });

  it('renders mapped invalid response message', () => {
    allAccountsState.error = {
      response: {
        status: 409,
        data: {
          code: 'ACCOUNT_ALREADY_ACTIVE',
          message: 'Account is already active'
        }
      }
    };

    render(
      <MemoryRouter>
        <AccountAdminListPage />
      </MemoryRouter>
    );

    expect(screen.getByText('Account is already active')).toBeInTheDocument();
  });
});
