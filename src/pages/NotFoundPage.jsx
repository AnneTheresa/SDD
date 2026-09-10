import { Link } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <div className="not-found-page">
      <h2 className="not-found-title">
        Page not found
      </h2>
      <p className="muted not-found-copy">
        We're sorry, the page you are looking for does not exist or has been moved.
      </p>
      <Link
        to="/"
        className="button-link not-found-link"
      >
        Back to Overview
      </Link>
    </div>
  );
}
