import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CustomerProfilePage } from './CustomerProfilePage';

const getCustomer = vi.fn();
const updateCustomer = vi.fn();
const refetch = vi.fn();

const queryState = {
  isLoading: false,
  data: null,
  error: null,
  refetch
};

const authContext = {
  authState: {
    username: 'user@example.com',
    customerId: '42',
    roles: [],
    accessToken: 'token'
  },
  isAdmin: false
};

vi.mock('@tanstack/react-query', () => ({
  useQuery: () => queryState,
  useMutation: ({ mutationFn }) => ({
    isPending: false,
    mutateAsync: mutationFn
  })
}));

vi.mock('../api/customers', () => ({
  getCustomer: (...args) => getCustomer(...args),
  updateCustomer: (...args) => updateCustomer(...args)
}));

vi.mock('../auth/AuthContext', () => ({
  useAuth: () => authContext
}));

vi.mock('../api/axiosClient', () => ({
  mapAxiosError: (err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred'
  })
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <CustomerProfilePage />
    </MemoryRouter>
  );
}

describe('CustomerProfilePage', () => {
  beforeEach(() => {
    queryState.isLoading = false;
    queryState.data = null;
    queryState.error = null;
    authContext.isAdmin = false;
    authContext.authState = {
      username: 'user@example.com',
      customerId: '42',
      roles: [],
      accessToken: 'token'
    };
    getCustomer.mockReset();
    updateCustomer.mockReset();
    refetch.mockReset();
  });

  it('shows a loading banner while the customer data is fetching', () => {
    queryState.isLoading = true;
    renderPage();
    expect(screen.getByText('Loading profile\u2026')).toBeInTheDocument();
  });

  it('shows an error banner when the query fails', () => {
    queryState.error = { response: { data: { message: 'Customer not found' } } };
    renderPage();
    expect(screen.getByText('Customer not found')).toBeInTheDocument();
  });

  it('renders the customer name and type in the identity card', () => {
    queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    renderPage();
    expect(screen.getByText('Jane Doe')).toBeInTheDocument();
    expect(screen.getByText('Personal Account')).toBeInTheDocument();
  });

  it('shows the email from authState as a read-only field', () => {
    queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    renderPage();
    const emailInput = screen.getByLabelText('Email');
    expect(emailInput).toHaveValue('user@example.com');
    expect(emailInput).toBeDisabled();
  });

  it('shows the customer name as a read-only input field', () => {
    queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    renderPage();
    const nameInput = screen.getByLabelText('Full Name');
    expect(nameInput).toHaveValue('Jane Doe');
    expect(nameInput).toBeDisabled();
  });

  it('shows the support hint under the email field', () => {
    queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    renderPage();
    expect(screen.getByText('Contact support to update this information.')).toBeInTheDocument();
  });

  it('does not show an Edit button for the Identity section', () => {
    queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    renderPage();
    // Only the address section should have an Edit button
    expect(screen.getAllByRole('button', { name: 'Edit' })).toHaveLength(1);
  });

  it('shows the customer address in the Location section', () => {
    queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    renderPage();
    expect(screen.getByLabelText('Address')).toHaveValue('1 Main St');
  });

  describe('address editing', () => {
    beforeEach(() => {
      queryState.data = { name: 'Jane Doe', type: 'PERSON', address: '1 Main St' };
    });

    it('enables the address input and shows Update/Cancel after clicking the Location Edit button', () => {
      renderPage();
      fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);
      expect(screen.getByRole('button', { name: 'Update' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'Cancel' })).toBeInTheDocument();
    });

    it('hides the address edit form after clicking Cancel', () => {
      renderPage();
      fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);
      fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
      expect(screen.queryByRole('button', { name: 'Update' })).not.toBeInTheDocument();
    });

    it('shows a success banner after address is saved successfully', async () => {
      updateCustomer.mockResolvedValue({});
      renderPage();
      fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);
      fireEvent.click(screen.getByRole('button', { name: 'Update' }));
      await waitFor(() => {
        expect(screen.getByText('Address updated.')).toBeInTheDocument();
      });
    });

    it('calls updateCustomer with the address payload on save', async () => {
      updateCustomer.mockResolvedValue({});
      renderPage();
      fireEvent.click(screen.getAllByRole('button', { name: 'Edit' })[0]);
      fireEvent.change(screen.getByLabelText('Address'), { target: { value: '99 New Road' } });
      fireEvent.click(screen.getByRole('button', { name: 'Update' }));
      await waitFor(() => {
        expect(updateCustomer).toHaveBeenCalledWith('42', { address: '99 New Road' });
      });
    });
  });
});
