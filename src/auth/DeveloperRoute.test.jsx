import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DeveloperRoute } from './DeveloperRoute';

const mockUseAuth = vi.fn();

vi.mock('./AuthContext', () => ({
  useAuth: () => mockUseAuth(),
}));

function renderRoute(initialEntry = '/developer/chat') {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route element={<DeveloperRoute />}>
          <Route path="/developer/chat" element={<div>Developer chat</div>} />
        </Route>
        <Route path="/" element={<div>Home</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('DeveloperRoute', () => {
  beforeEach(() => {
    mockUseAuth.mockReset();
  });

  it('renders developer content for a developer role', () => {
    mockUseAuth.mockReturnValue({ isDeveloper: true });

    renderRoute();

    expect(screen.getByText('Developer chat')).toBeInTheDocument();
  });

  it('redirects users without a developer role', () => {
    mockUseAuth.mockReturnValue({ isDeveloper: false });

    renderRoute();

    expect(screen.getByText('Home')).toBeInTheDocument();
    expect(screen.queryByText('Developer chat')).not.toBeInTheDocument();
  });
});
