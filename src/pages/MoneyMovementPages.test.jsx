import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DepositPage } from './DepositPage';
import { TransferPage } from './TransferPage';
import { WithdrawPage } from './WithdrawPage';

const depositMutateAsync = vi.fn();
const withdrawMutateAsync = vi.fn();
const transferMutateAsync = vi.fn();
const recategoriseMutateAsync = vi.fn();

vi.mock('../hooks/useDeposit', () => ({
  useDeposit: () => ({
    isPending: false,
    mutateAsync: depositMutateAsync
  })
}));

vi.mock('../hooks/useWithdraw', () => ({
  useWithdraw: () => ({
    isPending: false,
    mutateAsync: withdrawMutateAsync
  })
}));

vi.mock('../hooks/useGroup3', () => ({
  useRecategoriseTransaction: () => ({
    mutateAsync: recategoriseMutateAsync
  })
}));

vi.mock('@tanstack/react-query', () => ({
  useMutation: () => ({
    isPending: false,
    mutateAsync: transferMutateAsync
  }),
  useQuery: () => ({
    data: [
      { accountId: 10, accountType: 'CHEQUING', balance: '500.00' },
      { accountId: 12, accountType: 'SAVINGS', balance: '250.00' }
    ],
    isLoading: false
  })
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => ({ authState: { customerId: 1 } })
}));

function renderDepositPage() {
  return render(
    <MemoryRouter initialEntries={['/accounts/12/deposit']}>
      <Routes>
        <Route path="/accounts/:accountId/deposit" element={<DepositPage />} />
      </Routes>
    </MemoryRouter>
  );
}

function renderWithdrawPage() {
  return render(
    <MemoryRouter initialEntries={['/accounts/15/withdraw']}>
      <Routes>
        <Route path="/accounts/:accountId/withdraw" element={<WithdrawPage />} />
      </Routes>
    </MemoryRouter>
  );
}

function renderTransferPage(initialEntry = '/accounts/transfer') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/accounts/transfer" element={<TransferPage />} />
      </Routes>
    </MemoryRouter>
  );
}

describe('money movement pages', () => {
  beforeEach(() => {
    depositMutateAsync.mockReset();
    withdrawMutateAsync.mockReset();
    transferMutateAsync.mockReset();
    recategoriseMutateAsync.mockReset();
    recategoriseMutateAsync.mockResolvedValue({});
  });

  it('shows only the success banner after a deposit completes', async () => {
    depositMutateAsync.mockResolvedValue({
      message: 'Deposit completed',
      account: { debugMarker: 'account-json-marker' },
      transaction: { debugMarker: 'transaction-json-marker' }
    });

    renderDepositPage();

    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '55.00' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Pay in' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Deposit' }));

    expect(await screen.findByText('Deposit completed')).toBeInTheDocument();
    expect(screen.queryByText('account-json-marker')).not.toBeInTheDocument();
    expect(screen.queryByText('transaction-json-marker')).not.toBeInTheDocument();
    expect(depositMutateAsync).toHaveBeenCalledWith({
      accountId: '12',
      amount: '55.00',
      description: 'Pay in',
      category: ''
    });
  });

  it('shows only the success banner after a withdrawal completes', async () => {
    withdrawMutateAsync.mockResolvedValue({
      message: 'Withdrawal completed',
      account: { debugMarker: 'withdraw-account-json-marker' },
      transaction: { transactionId: 91, debugMarker: 'withdraw-transaction-json-marker' }
    });

    renderWithdrawPage();

    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '21.50' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Cash out' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Withdrawal' }));

    expect(await screen.findByText('Withdrawal completed')).toBeInTheDocument();
    expect(screen.queryByText('withdraw-account-json-marker')).not.toBeInTheDocument();
    expect(screen.queryByText('withdraw-transaction-json-marker')).not.toBeInTheDocument();
  });

  it('blocks transfers between the same account', async () => {
    renderTransferPage();

    fireEvent.change(screen.getByLabelText('From Account'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('To Account'), { target: { value: '10' } });
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '15.00' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Transfer' }));

    expect(await screen.findByText('Source and destination accounts must be different.')).toBeInTheDocument();
    expect(transferMutateAsync).not.toHaveBeenCalled();
  });

  it('shows only the success banner after a transfer completes', async () => {
    transferMutateAsync.mockResolvedValue({
      message: 'Transfer completed',
      fromAccount: { debugMarker: 'from-account-json-marker' },
      toAccount: { debugMarker: 'to-account-json-marker' },
      debitTransaction: { transactionId: 200, debugMarker: 'debit-json-marker' },
      creditTransaction: { debugMarker: 'credit-json-marker' }
    });

    renderTransferPage('/accounts/transfer?fromAccountId=10');

    fireEvent.change(screen.getByLabelText('To Account'), { target: { value: '12' } });
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '15.00' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Move funds' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Transfer' }));

    expect(await screen.findByText('Transfer completed')).toBeInTheDocument();
    await waitFor(() => {
      expect(transferMutateAsync).toHaveBeenCalledWith({
        fromAccountId: 10,
        toAccountId: 12,
        amount: '15.00',
        description: 'Move funds',
        category: ''
      });
    });
    expect(screen.queryByText('from-account-json-marker')).not.toBeInTheDocument();
    expect(screen.queryByText('to-account-json-marker')).not.toBeInTheDocument();
    expect(screen.queryByText('debit-json-marker')).not.toBeInTheDocument();
    expect(screen.queryByText('credit-json-marker')).not.toBeInTheDocument();
  });

  it('shows controlled blocked message when withdrawal is blocked on frozen account', async () => {
    withdrawMutateAsync.mockRejectedValue({
      response: {
        status: 409,
        data: {
          code: 'ACCOUNT_TEMPORARILY_RESTRICTED',
          message: 'internal-message-should-not-leak'
        }
      }
    });

    renderWithdrawPage();

    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '21.50' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Cash out' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Withdrawal' }));

    expect(await screen.findByText('This account is temporarily restricted. Please contact support.')).toBeInTheDocument();
  });

  it('shows controlled blocked message when transfer is blocked on frozen account', async () => {
    transferMutateAsync.mockRejectedValue({
      response: {
        status: 409,
        data: {
          code: 'ACCOUNT_TEMPORARILY_RESTRICTED',
          message: 'internal-message-should-not-leak'
        }
      }
    });

    renderTransferPage('/accounts/transfer?fromAccountId=10');

    fireEvent.change(screen.getByLabelText('To Account'), { target: { value: '12' } });
    fireEvent.change(screen.getByLabelText('Amount'), { target: { value: '15.00' } });
    fireEvent.change(screen.getByLabelText('Description'), { target: { value: 'Move funds' } });
    fireEvent.click(screen.getByRole('button', { name: 'Submit Transfer' }));

    expect(await screen.findByText('This account is temporarily restricted. Please contact support.')).toBeInTheDocument();
  });
});