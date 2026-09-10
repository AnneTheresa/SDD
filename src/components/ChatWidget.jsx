import { useEffect, useRef, useState } from 'react';
import { useSavingsChat, useConfirmAgentAction } from '../hooks/useSavingsChat';
import { mapAxiosError } from '../api/axiosClient';

const WELCOME_MESSAGE = {
  id: 'welcome',
  role: 'assistant',
  text: "Hi, I'm your savings assistant. Ask me about your spending, your savings goals, or general ways to save more.",
  basedOn: [],
  limitedData: false,
  blocked: false
};

let messageIdCounter = 0;
function nextMessageId() {
  messageIdCounter += 1;
  return `msg-${messageIdCounter}`;
}

/**
 * Floating Savings Insight Chatbot widget (BRD 6.1). Mounted once for authenticated
 * customers in AppLayout so it's available from any page. The backend agent decides
 * for itself what customer data or knowledge base articles to use per question — this
 * component just renders the conversation and surfaces the "based on" / limited-data /
 * blocked signals the backend sends back with each reply.
 */
export function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [draft, setDraft] = useState('');
  const [messages, setMessages] = useState([WELCOME_MESSAGE]);
  const chatMutation = useSavingsChat();
  const confirmMutation = useConfirmAgentAction();
  const scrollRef = useRef(null);

  useEffect(() => {
    if (!isOpen || !scrollRef.current) {
      return;
    }
    scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
  }, [isOpen, messages, chatMutation.isPending]);

  function handleToggle() {
    setIsOpen((open) => !open);
  }

  function handleSubmit(event) {
    event.preventDefault();
    const text = draft.trim();
    if (!text || chatMutation.isPending) {
      return;
    }

    setMessages((current) => [...current, { id: nextMessageId(), role: 'user', text }]);
    setDraft('');

    chatMutation.mutate(text, {
      onSuccess: (data) => {
        setMessages((current) => [
          ...current,
          {
            id: nextMessageId(),
            role: 'assistant',
            text: data?.response || "I don't have a response for that right now.",
            basedOn: data?.based_on || [],
            limitedData: Boolean(data?.limited_data),
            blocked: Boolean(data?.blocked),
            pendingConfirmation: data?.pending_confirmation || null
          }
        ]);
      },
      onError: (error) => {
        const mapped = mapAxiosError(error);
        setMessages((current) => [
          ...current,
          {
            id: nextMessageId(),
            role: 'assistant',
            text: mapped.message || "I'm unable to respond right now. Please try again in a moment.",
            basedOn: [],
            limitedData: false,
            blocked: false,
            isError: true
          }
        ]);
      }
    });
  }

  function handleConfirmAction(messageId, token) {
    confirmMutation.mutate(token, {
      onSuccess: (data) => {
        setMessages((current) => current.map((message) =>
          message.id === messageId
            ? { ...message, pendingConfirmation: null, confirmedResultText: data?.message || 'This action was completed.' }
            : message
        ));
      },
      onError: (error) => {
        const mapped = mapAxiosError(error);
        setMessages((current) => current.map((message) =>
          message.id === messageId
            ? { ...message, pendingConfirmation: { ...message.pendingConfirmation, errorText: mapped.message || 'This confirmation could not be completed.' } }
            : message
        ));
      }
    });
  }

  function handleDismissAction(messageId) {
    setMessages((current) => current.map((message) =>
      message.id === messageId ? { ...message, pendingConfirmation: null } : message
    ));
  }

  return (
    <div className="chat-widget">
      {isOpen && (
        <section className="chat-panel" role="dialog" aria-label="Savings assistant chat" aria-modal="false">
          <div className="chat-panel-header">
            <div>
              <p className="chat-panel-title">Savings Assistant</p>
              <p className="chat-panel-subtitle muted">Ask about your spending &amp; savings</p>
            </div>
            <button
              type="button"
              className="chat-panel-close"
              onClick={handleToggle}
              aria-label="Close chat"
            >
              ✕
            </button>
          </div>

          <div className="chat-messages" ref={scrollRef}>
            {messages.map((message) => (
              <div
                key={message.id}
                className={`chat-bubble-row ${message.role === 'user' ? 'user' : 'assistant'}`}
              >
                <div
                  className={`chat-bubble ${message.role === 'user' ? 'user' : 'assistant'}${message.isError ? ' error' : ''}${message.blocked ? ' blocked' : ''}`}
                >
                  <p className="chat-bubble-text">{message.text}</p>
                  {message.role === 'assistant' && message.basedOn && message.basedOn.length > 0 && (
                    <div className="chat-citation-list">
                      {message.basedOn.map((citation) => (
                        <span key={citation} className="chat-citation-chip">{citation}</span>
                      ))}
                    </div>
                  )}
                  {message.role === 'assistant' && message.limitedData && !message.blocked && !message.isError && (
                    <p className="chat-bubble-note">General guidance — limited personal data used.</p>
                  )}
                  {message.role === 'assistant' && message.confirmedResultText && (
                    <p className="chat-bubble-note">{message.confirmedResultText}</p>
                  )}
                  {message.role === 'assistant' && message.pendingConfirmation && (
                    <div className="chat-confirmation-card">
                      <p className="chat-confirmation-summary">{message.pendingConfirmation.summary}</p>
                      {message.pendingConfirmation.errorText && (
                        <p className="chat-confirmation-error">{message.pendingConfirmation.errorText}</p>
                      )}
                      <div className="chat-confirmation-actions">
                        <button
                          type="button"
                          onClick={() => handleConfirmAction(message.id, message.pendingConfirmation.token)}
                          disabled={confirmMutation.isPending}
                        >
                          Confirm
                        </button>
                        <button
                          type="button"
                          className="chat-confirmation-dismiss"
                          onClick={() => handleDismissAction(message.id)}
                          disabled={confirmMutation.isPending}
                        >
                          Dismiss
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            ))}
            {chatMutation.isPending && (
              <div className="chat-bubble-row assistant">
                <div className="chat-bubble assistant chat-bubble-typing">
                  <span className="chat-typing-dot" />
                  <span className="chat-typing-dot" />
                  <span className="chat-typing-dot" />
                </div>
              </div>
            )}
          </div>

          <form className="chat-input-row" onSubmit={handleSubmit}>
            <input
              type="text"
              value={draft}
              onChange={(event) => setDraft(event.target.value)}
              placeholder="Ask about your savings..."
              maxLength={1000}
              disabled={chatMutation.isPending}
              aria-label="Message"
            />
            <button type="submit" disabled={chatMutation.isPending || !draft.trim()}>
              Send
            </button>
          </form>
        </section>
      )}

      <button
        type="button"
        className="chat-fab"
        onClick={handleToggle}
        aria-label={isOpen ? 'Close savings assistant chat' : 'Open savings assistant chat'}
      >
        {isOpen ? (
          '✕'
        ) : (
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
          </svg>
        )}
      </button>
    </div>
  );
}
