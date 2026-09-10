import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { AdminRoute } from './AdminRoute';

const mockUseAuth = vi.fn();

vi.mock('./AuthContext', () => ({
  useAuth: () => mockUseAuth()
}));

function renderAdminRoute(initialEntry) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route element={<AdminRoute />}>
          <Route path="/admin/accounts" element={<div>Admin accounts</div>} />
        </Route>
        <Route path="/" element={<div>Home page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('AdminRoute', () => {
  it('redirects non-admin users to home', () => {
    mockUseAuth.mockReturnValue({ isAdmin: false });

    renderAdminRoute('/admin/accounts');

    expect(screen.getByText('Home page')).toBeInTheDocument();
    expect(screen.queryByText('Admin accounts')).not.toBeInTheDocument();
  });

  it('renders admin content for admins', () => {
    mockUseAuth.mockReturnValue({ isAdmin: true });

    renderAdminRoute('/admin/accounts');

    expect(screen.getByText('Admin accounts')).toBeInTheDocument();
  });
});
