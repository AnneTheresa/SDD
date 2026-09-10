import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AccountDetailPage } from '../../pages/AccountDetailPage';

const freezeMutateAsync = vi.fn();
const unfreezeMutateAsync = vi.fn();
const invalidateQueries = vi.fn();

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

const historyState = {
  data: { events: [] },
  isLoading: false
};

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({ isAdmin: true })
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
  freezeAccount: (...args) => freezeMutateAsync(...args),
  unfreezeAccount: (...args) => unfreezeMutateAsync(...args),
  getAccountControlHistory: vi.fn(() => historyState.data)
}));

vi.mock('@tanstack/react-query', () => ({
  useMutation: ({ mutationFn }) => ({
    isPending: false,
    mutateAsync: mutationFn
  }),
  useQueryClient: () => ({
    invalidateQueries
  }),
  useQuery: ({ queryKey }) => {
    if (queryKey[0] === 'account-control-history') {
      return historyState;
    }
    return { data: [], isLoading: false };
  }
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

describe('AccountDetailPage account control', () => {
  beforeEach(() => {
    freezeMutateAsync.mockReset();
    unfreezeMutateAsync.mockReset();
    invalidateQueries.mockReset();
    accountState.data.status = 'ACTIVE';
    historyState.data = { events: [] };
  });

  it('requires a freeze reason before enabling freeze submit', () => {
    renderPage();

    const freezeButton = screen.getByRole('button', { name: 'Freeze Account' });
    expect(freezeButton).toBeDisabled();

    fireEvent.change(screen.getByLabelText('Freeze Reason'), { target: { value: 'Risk review' } });
    expect(screen.getByRole('button', { name: 'Freeze Account' })).not.toBeDisabled();
  });

  it('allows unfreeze with optional note', async () => {
    accountState.data.status = 'FROZEN';
    unfreezeMutateAsync.mockResolvedValue({ newStatus: 'ACTIVE' });

    renderPage();

    fireEvent.change(screen.getByLabelText('Release Note (optional)'), { target: { value: 'resolved' } });
    fireEvent.click(screen.getByRole('button', { name: 'Unfreeze Account' }));

    await waitFor(() => {
      expect(unfreezeMutateAsync).toHaveBeenCalledWith({
        accountId: 1001,
        notes: 'resolved'
      });
    });
  });

  it('renders invalid freeze or unfreeze response message', async () => {
    freezeMutateAsync.mockRejectedValue({
      response: {
        status: 409,
        data: { code: 'ACCOUNT_ALREADY_FROZEN', message: 'Account is already frozen' }
      }
    });

    renderPage();

    fireEvent.change(screen.getByLabelText('Freeze Reason'), { target: { value: 'Risk review' } });
    fireEvent.click(screen.getByRole('button', { name: 'Freeze Account' }));

    expect(await screen.findByText('Account is already frozen')).toBeInTheDocument();
  });
});
