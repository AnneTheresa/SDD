import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CustomerEditPage } from './CustomerEditPage';

const getCustomer = vi.fn();
const updateCustomer = vi.fn();
const refetch = vi.fn();

const queryState = {
  isLoading: false,
  data: null,
  error: null,
  refetch
};

const mutationState = { isPending: false };

vi.mock('@tanstack/react-query', () => ({
  useQuery: () => queryState,
  useMutation: ({ mutationFn }) => ({
    get isPending() {
      return mutationState.isPending;
    },
    mutateAsync: mutationFn
  })
}));

vi.mock('../api/customers', () => ({
  getCustomer: (...args) => getCustomer(...args),
  updateCustomer: (...args) => updateCustomer(...args)
}));

vi.mock('../api/axiosClient', () => ({
  mapAxiosError: (err) => ({
    message: err?.response?.data?.message || err?.message || 'An error occurred'
  })
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/customer/55/edit']}>
      <Routes>
        <Route path="/customer/:customerId/edit" element={<CustomerEditPage />} />
        <Route path="/customer/:customerId" element={<div>Customer Detail</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('CustomerEditPage', () => {
  beforeEach(() => {
    queryState.isLoading = false;
    queryState.data = null;
    queryState.error = null;
    mutationState.isPending = false;
    getCustomer.mockReset();
    updateCustomer.mockReset();
    refetch.mockReset();
  });

  it('shows a loading banner while the customer query is in flight', () => {
    queryState.isLoading = true;
    renderPage();
    expect(screen.getByText('Loading customer...')).toBeInTheDocument();
  });

  it('pre-populates the form fields with the loaded customer data', () => {
    queryState.data = { name: 'Jane Doe', address: '1 Main St', type: 'PERSON' };
    renderPage();
    expect(screen.getByLabelText('Name')).toHaveValue('Jane Doe');
    expect(screen.getByLabelText('Address')).toHaveValue('1 Main St');
    expect(screen.getByLabelText('Type')).toHaveValue('PERSON');
  });

  it('shows an error banner when the query fails', () => {
    queryState.error = { response: { data: { message: 'Customer not found' } } };
    renderPage();
    expect(screen.getByText('Customer not found')).toBeInTheDocument();
  });

  it('shows a success message and calls updateCustomer after the form is submitted', async () => {
    updateCustomer.mockResolvedValue({});
    queryState.data = { name: 'Jane Doe', address: '1 Main St', type: 'PERSON' };

    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(screen.getByText('Customer updated successfully.')).toBeInTheDocument();
    });
    expect(updateCustomer).toHaveBeenCalledWith('55', {
      name: 'Jane Doe',
      address: '1 Main St',
      type: 'PERSON'
    });
  });

  it('shows an error banner when the mutation fails', async () => {
    updateCustomer.mockRejectedValue({
      response: { data: { message: 'Update failed' } }
    });
    queryState.data = { name: 'Jane Doe', address: '1 Main St', type: 'PERSON' };

    renderPage();

    fireEvent.click(screen.getByRole('button', { name: 'Save Changes' }));

    await waitFor(() => {
      expect(screen.getByText('Update failed')).toBeInTheDocument();
    });
  });

  it('disables the Save Changes button while the mutation is pending', () => {
    mutationState.isPending = true;
    queryState.data = { name: 'Jane Doe', address: '1 Main St', type: 'PERSON' };

    renderPage();

    expect(screen.getByRole('button', { name: 'Save Changes' })).toBeDisabled();
  });

  it('includes a Back to Customer link pointing to the customer detail page', () => {
    queryState.data = { name: 'Jane Doe', address: '1 Main St', type: 'PERSON' };
    renderPage();
    expect(screen.getByRole('link', { name: 'Back to Customer' })).toHaveAttribute('href', '/customer/55');
  });
});
