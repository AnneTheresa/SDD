import { fireEvent, render, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ChatWidget } from '../../components/ChatWidget';

const mutationState = {
  isPending: false,
  mutate: vi.fn()
};

const confirmMutationState = {
  isPending: false,
  mutate: vi.fn()
};

vi.mock('../../hooks/useSavingsChat', () => ({
  useSavingsChat: () => mutationState,
  useConfirmAgentAction: () => confirmMutationState
}));

describe('ChatWidget', () => {
  beforeEach(() => {
    mutationState.isPending = false;
    mutationState.mutate.mockReset();
    confirmMutationState.isPending = false;
    confirmMutationState.mutate.mockReset();
  });

  it('is collapsed to just the launcher button by default', () => {
    render(<ChatWidget />);
    expect(screen.getByRole('button', { name: /open savings assistant chat/i })).toBeInTheDocument();
    expect(screen.queryByRole('dialog', { name: /savings assistant chat/i })).not.toBeInTheDocument();
  });

  it('opens the panel with a welcome message when the launcher is clicked', () => {
    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    expect(screen.getByRole('dialog', { name: /savings assistant chat/i })).toBeInTheDocument();
    expect(screen.getByText(/i'm your savings assistant/i)).toBeInTheDocument();
  });

  it('closes the panel when the close button is clicked', () => {
    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    fireEvent.click(screen.getByRole('button', { name: /close chat/i }));
    expect(screen.queryByRole('dialog', { name: /savings assistant chat/i })).not.toBeInTheDocument();
  });

  it('sends the typed message and renders the reply with its citations', () => {
    mutationState.mutate.mockImplementation((text, { onSuccess }) => {
      onSuccess({
        response: 'Based on your dining spend, consider cooking in more.',
        based_on: ['Your recent transaction history'],
        limited_data: false,
        blocked: false
      });
    });

    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));

    const input = screen.getByLabelText('Message');
    fireEvent.change(input, { target: { value: 'How can I save more on dining out?' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(mutationState.mutate).toHaveBeenCalledWith(
      'How can I save more on dining out?',
      expect.objectContaining({ onSuccess: expect.any(Function), onError: expect.any(Function) })
    );
    expect(screen.getByText('How can I save more on dining out?')).toBeInTheDocument();
    expect(screen.getByText(/consider cooking in more/i)).toBeInTheDocument();
    expect(screen.getByText('Your recent transaction history')).toBeInTheDocument();
    expect(input.value).toBe('');
  });

  it('shows a limited-data note when the response falls back to general guidance', () => {
    mutationState.mutate.mockImplementation((text, { onSuccess }) => {
      onSuccess({
        response: 'General savings tip: automate a transfer right after payday.',
        based_on: [],
        limited_data: true,
        blocked: false
      });
    });

    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    fireEvent.change(screen.getByLabelText('Message'), { target: { value: 'How do I save more?' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(screen.getByText(/limited personal data used/i)).toBeInTheDocument();
  });

  it('shows a friendly fallback message when the request fails', () => {
    mutationState.mutate.mockImplementation((text, { onError }) => {
      onError({ response: { data: { message: 'Service unavailable' } } });
    });

    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    fireEvent.change(screen.getByLabelText('Message'), { target: { value: 'Hello' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(screen.getByText('Service unavailable')).toBeInTheDocument();
  });

  it('does not submit a blank message', () => {
    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
    expect(mutationState.mutate).not.toHaveBeenCalled();
  });

  it('disables the input and send button while a request is pending', () => {
    mutationState.isPending = true;
    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    expect(screen.getByLabelText('Message')).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Send' })).toBeDisabled();
  });

  it('renders a confirmation card when the response includes pending_confirmation, and confirms it', () => {
    mutationState.mutate.mockImplementation((text, { onSuccess }) => {
      onSuccess({
        response: "I've prepared that transfer for you to confirm.",
        based_on: [],
        limited_data: false,
        blocked: false,
        pending_confirmation: {
          token: 'tok-1',
          action_type: 'TRANSFER',
          summary: 'Transfer $50.00 from your CHECKING account (#1) to your SAVINGS account (#2).',
          expires_at: '2026-08-26T12:05:00'
        }
      });
    });
    confirmMutationState.mutate.mockImplementation((token, { onSuccess }) => {
      onSuccess({ message: 'Transfer complete' });
    });

    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    fireEvent.change(screen.getByLabelText('Message'), { target: { value: 'transfer $50 from checking to savings' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    expect(screen.getByText(/Transfer \$50\.00 from your CHECKING account/i)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /confirm/i }));

    expect(confirmMutationState.mutate).toHaveBeenCalledWith(
      'tok-1',
      expect.objectContaining({ onSuccess: expect.any(Function), onError: expect.any(Function) })
    );
    expect(screen.getByText('Transfer complete')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /confirm/i })).not.toBeInTheDocument();
  });

  it('dismisses a pending confirmation without calling the confirm endpoint', () => {
    mutationState.mutate.mockImplementation((text, { onSuccess }) => {
      onSuccess({
        response: "I've prepared that transfer for you to confirm.",
        based_on: [],
        limited_data: false,
        blocked: false,
        pending_confirmation: {
          token: 'tok-2',
          action_type: 'TRANSFER',
          summary: 'Transfer $10.00 from your CHECKING account (#1) to your SAVINGS account (#2).',
          expires_at: '2026-08-26T12:05:00'
        }
      });
    });

    render(<ChatWidget />);
    fireEvent.click(screen.getByRole('button', { name: /open savings assistant chat/i }));
    fireEvent.change(screen.getByLabelText('Message'), { target: { value: 'transfer $10 from checking to savings' } });
    fireEvent.click(screen.getByRole('button', { name: 'Send' }));

    fireEvent.click(screen.getByRole('button', { name: /dismiss/i }));

    expect(confirmMutationState.mutate).not.toHaveBeenCalled();
    expect(screen.queryByRole('button', { name: /confirm/i })).not.toBeInTheDocument();
  });
});
