import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import { ThemeProvider } from './theme/ThemeContext';

const mockLogout = vi.fn();
const mockNavigate = vi.fn();

const authContext = {
  authState: {
    username: 'user@example.com',
    customerId: '42',
    roles: [],
    accessToken: 'token'
  },
  isAdmin: false,
  isAuthenticated: true,
  logout: mockLogout
};

const accountsQueryState = {
  isLoading: false,
  data: [],
  error: null
};

vi.mock('./auth/AuthContext', () => ({
  useAuth: () => authContext
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('./hooks/useListCustomerAccounts', () => ({
  useListCustomerAccounts: () => accountsQueryState
}));

vi.mock('./auth/ProtectedRoute', () => ({ ProtectedRoute: () => null }));

vi.mock('./components/ChatWidget', () => ({ ChatWidget: () => null }));

vi.mock('./pages/HomePage', () => ({ HomePage: () => null }));
vi.mock('./pages/LoginPage', () => ({ LoginPage: () => null }));
vi.mock('./pages/RegisterPage', () => ({ RegisterPage: () => null }));
vi.mock('./pages/PasswordResetPage', () => ({ PasswordResetPage: () => null }));
vi.mock('./pages/CustomerCreatePage', () => ({ CustomerCreatePage: () => null }));
vi.mock('./pages/CustomerDetailPage', () => ({ CustomerDetailPage: () => null }));
vi.mock('./pages/CustomerEditPage', () => ({ CustomerEditPage: () => null }));
vi.mock('./pages/CustomerProfilePage', () => ({ CustomerProfilePage: () => null }));
vi.mock('./pages/AccountListPage', () => ({ AccountListPage: () => null }));
vi.mock('./pages/CreateAccountPage', () => ({ CreateAccountPage: () => null }));
vi.mock('./pages/AccountDetailPage', () => ({ AccountDetailPage: () => null }));
vi.mock('./pages/DepositPage', () => ({ DepositPage: () => null }));
vi.mock('./pages/WithdrawPage', () => ({ WithdrawPage: () => null }));
vi.mock('./pages/TransferPage', () => ({ TransferPage: () => null }));
vi.mock('./pages/TransactionHistoryPage', () => ({ TransactionHistoryPage: () => null }));
vi.mock('./pages/StandingOrdersPage', () => ({ StandingOrdersPage: () => null }));
vi.mock('./pages/MonthlyStatementPage', () => ({ MonthlyStatementPage: () => null }));
vi.mock('./pages/SpendingInsightsPage', () => ({ SpendingInsightsPage: () => null }));

function renderApp(initialEntry = '/') {
  return render(
    <ThemeProvider>
      <MemoryRouter initialEntries={[initialEntry]}>
        <App />
      </MemoryRouter>
    </ThemeProvider>
  );
}

describe('Navbar and Sub-Navbar (AppLayout)', () => {
  beforeEach(() => {
    authContext.authState = {
      username: 'user@example.com',
      customerId: '42',
      roles: [],
      accessToken: 'token'
    };
    authContext.isAdmin = false;
    authContext.isAuthenticated = true;
    accountsQueryState.isLoading = false;
    accountsQueryState.data = [];
    accountsQueryState.error = null;
    mockLogout.mockReset();
    mockNavigate.mockReset();
  });

  describe('unauthenticated user', () => {
    beforeEach(() => {
      authContext.isAuthenticated = false;
      authContext.authState = {
        username: null,
        customerId: null,
        roles: [],
        accessToken: null
      };
    });

    it('shows the Voltio brand', () => {
      renderApp();
      expect(screen.getByRole('img', { name: /voltio/i })).toBeInTheDocument();
    });

    it('shows Login and Register links instead of the avatar', () => {
      renderApp();
      expect(screen.getByRole('link', { name: 'Login' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: 'Register' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: /open profile menu/i })).not.toBeInTheDocument();
    });

    it('does not render the sub-navbar', () => {
      renderApp();
      expect(screen.queryByRole('button', { name: 'Overview' })).not.toBeInTheDocument();
    });
  });

  describe('authenticated user — main navbar', () => {
    it('shows the avatar button with the first initial of the username', () => {
      renderApp();
      expect(screen.getByRole('button', { name: /open profile menu/i })).toBeInTheDocument();
      expect(screen.getByText('U')).toBeInTheDocument();
    });

    it('does not show Login or Register links', () => {
      renderApp();
      expect(screen.queryByRole('link', { name: 'Login' })).not.toBeInTheDocument();
      expect(screen.queryByRole('link', { name: 'Register' })).not.toBeInTheDocument();
    });
  });

  describe('authenticated user — sub-navbar items', () => {
    it('renders all feature sub-navbar buttons when the user has accounts', () => {
      accountsQueryState.data = [{ accountId: 1, accountType: 'SAVINGS', status: 'ACTIVE', balance: '100.00' }];
      renderApp();
      expect(screen.getByRole('button', { name: 'Overview' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Transactions' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Monthly Statement' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Spending Insights' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Standing Orders' })).toBeInTheDocument();
    });

    it('hides Transactions, Monthly Statement, Spending Insights and Standing Orders when the user has no accounts', () => {
      accountsQueryState.data = [];
      renderApp();
      expect(screen.getByRole('button', { name: 'Overview' })).toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Transactions' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Monthly Statement' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Spending Insights' })).not.toBeInTheDocument();
      expect(screen.queryByRole('button', { name: 'Standing Orders' })).not.toBeInTheDocument();
    });

    it('does not show Customer Profile in the sub-navbar', () => {
      renderApp();
      expect(screen.queryByRole('button', { name: 'Customer Profile' })).not.toBeInTheDocument();
    });

      it('shows My Accounts as a link when customerId is present', () => {
      renderApp();
        expect(screen.getByRole('link', { name: 'My Accounts' })).toBeInTheDocument();
        expect(screen.getByRole('link', { name: 'My Accounts' })).toHaveAttribute('href', '/customer/42/accounts');
    });

      it('does not show My Accounts when customerId is absent', () => {
      authContext.authState = { ...authContext.authState, customerId: null };
      renderApp();
        expect(screen.queryByRole('link', { name: 'My Accounts' })).not.toBeInTheDocument();
    });

    it('Overview is active and Customer Accounts is not active on a non-accounts page', () => {
      renderApp('/customer/42');
      const overview = screen.getByRole('button', { name: 'Overview' });
        const customerAccounts = screen.getByRole('link', { name: 'My Accounts' });
      expect(overview.className).toContain('active');
        expect(customerAccounts.className).not.toContain('active');
    });

    it('Customer Accounts is active and Overview is not active on the accounts list page', () => {
      renderApp('/customer/42/accounts');
      const overview = screen.getByRole('button', { name: 'Overview' });
        const customerAccounts = screen.getByRole('link', { name: 'My Accounts' });
      expect(overview.className).not.toContain('active');
        expect(customerAccounts.className).toContain('active');
    });

    it('Customer Accounts is active on the account detail page', () => {
      renderApp('/accounts/5');
      const overview = screen.getByRole('button', { name: 'Overview' });
        const customerAccounts = screen.getByRole('link', { name: 'My Accounts' });
      expect(overview.className).not.toContain('active');
        expect(customerAccounts.className).toContain('active');
    });

    it('no subnav link is highlighted on the customer profile page', () => {
      renderApp('/customer-profile');
      expect(screen.getByRole('button', { name: 'Overview' })).not.toHaveClass('active');
        expect(screen.getByRole('link', { name: 'My Accounts' })).not.toHaveClass('active');
    });
  });

  describe('Overview button', () => {
    it('navigates to / (HomePage) when Overview is clicked', () => {
      renderApp();
      fireEvent.click(screen.getByRole('button', { name: 'Overview' }));
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });

    it('does not show the Overview button for an admin', () => {
      authContext.isAdmin = true;
      authContext.authState = { ...authContext.authState, customerId: null, roles: ['ADMIN'] };
      renderApp();
      expect(screen.queryByRole('button', { name: 'Overview' })).not.toBeInTheDocument();
    });

    it('navigates to / when neither admin nor customerId', () => {
      authContext.authState = { ...authContext.authState, customerId: null };
      renderApp();
      fireEvent.click(screen.getByRole('button', { name: 'Overview' }));
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
  });

  describe('feature nav buttons — with an active account (from URL)', () => {
    it('navigates directly to transactions for the active account', () => {
      renderApp('/accounts/99/insights');
      fireEvent.click(screen.getByRole('button', { name: 'Transactions' }));
      expect(mockNavigate).toHaveBeenCalledWith('/accounts/99/transactions');
    });

    it('navigates directly to monthly statement for the active account', () => {
      renderApp('/accounts/99/transactions');
      fireEvent.click(screen.getByRole('button', { name: 'Monthly Statement' }));
      expect(mockNavigate).toHaveBeenCalledWith('/accounts/99/statements');
    });

    it('navigates directly to spending insights for the active account', () => {
      renderApp('/accounts/99/transactions');
      fireEvent.click(screen.getByRole('button', { name: 'Spending Insights' }));
      expect(mockNavigate).toHaveBeenCalledWith('/accounts/99/insights');
    });

    it('navigates directly to standing orders for the active account', () => {
      renderApp('/accounts/99/transactions');
      fireEvent.click(screen.getByRole('button', { name: 'Standing Orders' }));
      expect(mockNavigate).toHaveBeenCalledWith('/accounts/99/standing-orders');
    });
  });

  describe('feature nav buttons — no active account (opens account picker)', () => {
    it('opens the account picker modal when no account is active', () => {
      accountsQueryState.data = [{ accountId: 1, accountType: 'SAVINGS', status: 'ACTIVE', balance: '100.00' }];
      renderApp('/');
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: 'Transactions' }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      expect(screen.getByText('Select an Account')).toBeInTheDocument();
    });

    it('shows a loading state in the picker while accounts are fetching', () => {
      accountsQueryState.isLoading = true;
      accountsQueryState.data = null;
      renderApp('/');
      fireEvent.click(screen.getByRole('button', { name: 'Spending Insights' }));
      expect(screen.getByText('Loading accounts\u2026')).toBeInTheDocument();
    });

    it('shows account options in the picker and navigates on selection', () => {
      accountsQueryState.data = [
        { accountId: 7, accountType: 'SAVINGS', balance: '100.00' }
      ];
      renderApp('/');
      fireEvent.click(screen.getByRole('button', { name: 'Transactions' }));
      fireEvent.click(screen.getByRole('button', { name: /#7/i }));
      expect(mockNavigate).toHaveBeenCalledWith('/accounts/7/transactions');
    });

    it('closes the picker modal when the Close button is clicked', async () => {
      accountsQueryState.data = [{ accountId: 1, accountType: 'SAVINGS', status: 'ACTIVE', balance: '100.00' }];
      renderApp('/');
      fireEvent.click(screen.getByRole('button', { name: 'Transactions' }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: 'Close' }));
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });

    it('closes the picker modal when clicking the backdrop', async () => {
      accountsQueryState.data = [{ accountId: 1, accountType: 'SAVINGS', status: 'ACTIVE', balance: '100.00' }];
      renderApp('/');
      fireEvent.click(screen.getByRole('button', { name: 'Transactions' }));
      expect(screen.getByRole('dialog')).toBeInTheDocument();
      // Click the backdrop (parent of dialog)
      fireEvent.click(screen.getByRole('dialog').parentElement);
      await waitFor(() => {
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
      });
    });
  });

  describe('sub-navbar active state highlighting', () => {
    it('marks Overview as active when on a non-feature page', () => {
      accountsQueryState.data = [{ accountId: 1, accountType: 'SAVINGS', status: 'ACTIVE', balance: '100.00' }];
      renderApp('/');
      expect(screen.getByRole('button', { name: 'Overview' })).toHaveClass('active');
      expect(screen.getByRole('button', { name: 'Transactions' })).not.toHaveClass('active');
    });

    it('marks Transactions as active and Overview as inactive when on a transactions URL', () => {
      renderApp('/accounts/5/transactions');
      expect(screen.getByRole('button', { name: 'Transactions' })).toHaveClass('active');
      expect(screen.getByRole('button', { name: 'Overview' })).not.toHaveClass('active');
    });

    it('marks Monthly Statement as active when on a statements URL', () => {
      renderApp('/accounts/5/statements');
      expect(screen.getByRole('button', { name: 'Monthly Statement' })).toHaveClass('active');
    });

    it('marks Spending Insights as active when on an insights URL', () => {
      renderApp('/accounts/5/insights');
      expect(screen.getByRole('button', { name: 'Spending Insights' })).toHaveClass('active');
    });

    it('marks Standing Orders as active when on a standing-orders URL', () => {
      renderApp('/accounts/5/standing-orders');
      expect(screen.getByRole('button', { name: 'Standing Orders' })).toHaveClass('active');
    });
  });

  describe('profile dropdown', () => {
    it('opens the dropdown when the avatar button is clicked', () => {
      renderApp();
      expect(screen.queryByRole('button', { name: 'Profile' })).not.toBeInTheDocument();
      fireEvent.click(screen.getByRole('button', { name: /open profile menu/i }));
      expect(screen.getByRole('button', { name: 'Profile' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Log Out' })).toBeInTheDocument();
    });

    it('navigates to /customer-profile when Profile is clicked', () => {
      renderApp();
      fireEvent.click(screen.getByRole('button', { name: /open profile menu/i }));
      fireEvent.click(screen.getByRole('button', { name: 'Profile' }));
      expect(mockNavigate).toHaveBeenCalledWith('/customer-profile');
    });

    it('calls logout when Log Out is clicked', () => {
      renderApp();
      fireEvent.click(screen.getByRole('button', { name: /open profile menu/i }));
      fireEvent.click(screen.getByRole('button', { name: 'Log Out' }));
      expect(mockLogout).toHaveBeenCalled();
    });

    it('closes the dropdown when clicking outside the navbar-profile area', async () => {
      renderApp();
      fireEvent.click(screen.getByRole('button', { name: /open profile menu/i }));
      expect(screen.getByRole('button', { name: 'Profile' })).toBeInTheDocument();

      fireEvent.mouseDown(document.body);

      await waitFor(() => {
        expect(screen.queryByRole('button', { name: 'Profile' })).not.toBeInTheDocument();
      });
    });
  });
});
