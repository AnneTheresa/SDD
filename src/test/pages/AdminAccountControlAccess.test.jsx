import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountDetailPage } from '../../pages/AccountDetailPage';

const accountState = {
  data: {
    accountId: 1001,
    customerId: 42,
    accountType: 'CHECKING',
    status: 'ACTIVE',
    balance: '500.00'
  },
  isLoading: false,
  error: null
};

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({ isAdmin: false })
}));

vi.mock('../../hooks/useGetAccount', () => ({
  useGetAccount: () => accountState
}));

vi.mock('../../api/accounts', () => ({
  deleteAccount: vi.fn(),
  closeRrspAccount: vi.fn(),
  getGics: vi.fn(),
  openGic: vi.fn(),
  redeemGic: vi.fn(),
  adminUpdateInterestRate: vi.fn(),
  freezeAccount: vi.fn(),
  unfreezeAccount: vi.fn(),
  getAccountControlHistory: vi.fn(() => ({ events: [] }))
}));

vi.mock('@tanstack/react-query', () => ({
  useMutation: ({ mutationFn }) => ({
    isPending: false,
    mutateAsync: mutationFn
  }),
  useQueryClient: () => ({
    invalidateQueries: vi.fn()
  }),
  useQuery: () => ({ data: [], isLoading: false })
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/accounts/1001']}>
      <Routes>
        <Route path="/accounts/:accountId" element={<AccountDetailPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('Admin account control access', () => {
  beforeEach(() => {
    accountState.data.status = 'ACTIVE';
  });

  it('hides admin account-control actions for non-admin users', () => {
    renderPage();

    expect(screen.queryByText('Admin Management')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Freeze Account' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Unfreeze Account' })).not.toBeInTheDocument();
  });
});
