import { useState } from 'react';
import { mapAxiosError } from '../api/axiosClient';
import { useDeveloperChat } from '../hooks/useDeveloperChat';

function SourceList({ sources }) {
  if (!sources?.length) {
    return null;
  }

  return (
    <ul className="developer-chat-sources" aria-label="Sources">
      {sources.map((source) => (
        <li key={`${source.sourceType}-${source.path}-${source.revision}`}>
          {source.url ? (
            <a href={source.url} target="_blank" rel="noopener noreferrer">
              {source.path}
            </a>
          ) : (
            <span>{source.path}</span>
          )}
          <span className="muted">
            {' '}({source.sourceType}{source.revision ? ` · ${source.revision}` : ''})
          </span>
        </li>
      ))}
    </ul>
  );
}

export function DeveloperChatPage() {
  const [message, setMessage] = useState('');
  const [sessionId, setSessionId] = useState('');
  const [turns, setTurns] = useState([]);
  const [error, setError] = useState('');
  const chat = useDeveloperChat();

  async function handleSubmit(event) {
    event.preventDefault();
    const question = message.trim();
    if (!question || chat.isPending) {
      return;
    }

    setMessage('');
    setError('');
    setTurns((current) => [...current, { id: crypto.randomUUID(), question }]);
    try {
      const result = await chat.mutateAsync({ message: question, sessionId });
      setSessionId(result.sessionId);
      setTurns((current) => [
        ...current.slice(0, -1),
        {
          id: current.at(-1)?.id || crypto.randomUUID(),
          question,
          answer: result.answer,
          sources: result.sources || [],
          sourceStatus: result.sourceStatus,
        },
      ]);
    } catch (requestError) {
      const mapped = mapAxiosError(requestError);
      setError(mapped.message);
      setTurns((current) => current.slice(0, -1));
    }
  }

  function startNewConversation() {
    setTurns([]);
    setSessionId('');
    setError('');
    setMessage('');
  }

  return (
    <section className="developer-chat-page" aria-labelledby="developer-chat-title">
      <div className="section-header">
        <div>
          <h1 id="developer-chat-title">Developer assistant</h1>
          <p className="muted">
            Ask about the application using contributor documentation and the configured GitHub repository.
          </p>
        </div>
        <button type="button" className="secondary" onClick={startNewConversation}>
          New conversation
        </button>
      </div>

      <div className="developer-chat-transcript" aria-live="polite">
        {turns.length === 0 && (
          <p className="muted">
            Responses cite the sources they use. Repository-specific answers are withheld when evidence is unavailable.
          </p>
        )}
        {turns.map((turn) => (
          <article className="developer-chat-turn" key={turn.id}>
            <h2>You</h2>
            <p>{turn.question}</p>
            {turn.answer && (
              <div className="developer-chat-answer">
                <h2>Developer assistant</h2>
                <p>{turn.answer}</p>
                {turn.sourceStatus && (
                  <p className="muted" data-testid="source-status">
                    Evidence status: {turn.sourceStatus.toLowerCase().replaceAll('_', ' ')}
                  </p>
                )}
                <SourceList sources={turn.sources} />
              </div>
            )}
          </article>
        ))}
        {chat.isPending && <p role="status">Searching sources…</p>}
      </div>

      {error && <p className="banner error" role="alert">{error}</p>}

      <form className="developer-chat-form" onSubmit={handleSubmit}>
        <label htmlFor="developer-chat-message">Your development question</label>
        <textarea
          id="developer-chat-message"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          maxLength={2000}
          rows={4}
          required
        />
        <button type="submit" disabled={chat.isPending || !message.trim()}>
          Ask
        </button>
      </form>
    </section>
  );
}
