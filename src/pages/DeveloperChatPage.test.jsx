import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DeveloperChatPage } from './DeveloperChatPage';

const mockMutateAsync = vi.fn();

vi.mock('../hooks/useDeveloperChat', () => ({
  useDeveloperChat: () => ({
    mutateAsync: mockMutateAsync,
    isPending: false,
  }),
}));

describe('DeveloperChatPage', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  beforeEach(() => {
    mockMutateAsync.mockReset();
    vi.stubGlobal('crypto', { randomUUID: vi.fn(() => 'turn-1') });
  });

  it('renders a response with source citations and supports follow-up session ids', async () => {
    mockMutateAsync
      .mockResolvedValueOnce({
        sessionId: 'session-1',
        answer: 'The route lives in App.jsx.',
        sourceStatus: 'AVAILABLE',
        sources: [{
          sourceType: 'github',
          path: 'src/App.jsx',
          url: 'https://github.com/sofiarahul/banking-platform/blob/main/src/App.jsx',
          revision: 'branch:main',
        }],
      })
      .mockResolvedValueOnce({
        sessionId: 'session-1',
        answer: 'The route is role gated.',
        sourceStatus: 'AVAILABLE',
        sources: [],
      });

    render(<DeveloperChatPage />);
    const input = screen.getByLabelText('Your development question');
    fireEvent.change(input, { target: { value: 'Where is the route?' } });
    fireEvent.click(screen.getByRole('button', { name: 'Ask' }));

    expect(await screen.findByText('The route lives in App.jsx.')).toBeInTheDocument();
    const citation = screen.getByRole('link', { name: 'src/App.jsx' });
    expect(citation).toHaveAttribute('target', '_blank');
    expect(citation).toHaveAttribute('rel', 'noopener noreferrer');

    fireEvent.change(input, { target: { value: 'Is it gated?' } });
    fireEvent.click(screen.getByRole('button', { name: 'Ask' }));

    await waitFor(() => expect(mockMutateAsync).toHaveBeenCalledTimes(2));
    expect(mockMutateAsync).toHaveBeenNthCalledWith(2, {
      message: 'Is it gated?',
      sessionId: 'session-1',
    });
    expect(await screen.findByText('The route is role gated.')).toBeInTheDocument();
  });

  it('shows a mapped request error without persisting it as an assistant answer', async () => {
    mockMutateAsync.mockRejectedValueOnce({
      response: { status: 503, data: { code: 'SOURCE_UNAVAILABLE', message: 'Sources unavailable' } },
    });

    render(<DeveloperChatPage />);
    fireEvent.change(screen.getByLabelText('Your development question'), {
      target: { value: 'Explain the backend' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Ask' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Sources unavailable');
    expect(screen.queryByText('Developer assistant', { selector: 'h2' })).not.toBeInTheDocument();
  });
});
