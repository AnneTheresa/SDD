import { Link, useNavigate } from 'react-router-dom';
import { useEffect, useRef, useState } from 'react';
import { getCustomer } from '../api/customers';
import { mapAxiosError } from '../api/axiosClient';
import { useAuth } from '../auth/AuthContext';
import { emptyCustomerLookup } from '../types';
import offerImage1 from '../images/image_1.jpg';
import offerImage2 from '../images/image_2.jpg';
import offerImage3 from '../images/image_3.jpg';

const offers = [
  {
    title: 'Zero-Fee Starter Account',
    description: 'Open your first account with no monthly maintenance fee for the first 12 months.',
    badge: 'New',
    image: offerImage1
  },
  {
    title: 'Smart Savings Boost',
    description: 'Earn up to 4.10% variable APY with automated monthly round-up deposits.',
    badge: 'Popular',
    image: offerImage2
  },
  {
    title: 'Travel Rewards Card',
    description: 'Get 2x points on flights and hotels, plus no foreign transaction fees.',
    badge: 'Limited Offer',
    image: offerImage3
  }
];

export function HomePage() {
  const navigate = useNavigate();
  const { authState, isAuthenticated, isAdmin, rememberCustomerId } = useAuth();
  const currentYear = new Date().getFullYear();
  const [lookup, setLookup] = useState(emptyCustomerLookup);
  const [activeOfferIndex, setActiveOfferIndex] = useState(0);
  const [linkError, setLinkError] = useState(null);
  const [linkMessage, setLinkMessage] = useState(null);
  const [isLinkingCustomer, setIsLinkingCustomer] = useState(false);
  const intervalRef = useRef(null);

  function startSliderTimer() {
    window.clearInterval(intervalRef.current);
    intervalRef.current = window.setInterval(() => {
      setActiveOfferIndex((current) => (current + 1) % offers.length);
    }, 4500);
  }

  useEffect(() => {
    startSliderTimer();
    return () => window.clearInterval(intervalRef.current);
  }, []);

  function openCustomer() {
    if (lookup.customerId) {
      navigate(`/customer/${lookup.customerId}`);
    }
  }

  function openAccount() {
    if (lookup.accountId) {
      navigate(`/accounts/${lookup.accountId}`);
    }
  }

  async function linkCustomerProfile() {
    if (!lookup.customerId) {
      setLinkError({ message: 'Enter your customer ID first.' });
      setLinkMessage(null);
      return;
    }

    setIsLinkingCustomer(true);
    setLinkError(null);
    setLinkMessage(null);

    try {
      const customer = await getCustomer(lookup.customerId);
      rememberCustomerId(customer.customerId);
      setLinkMessage(`Customer profile ${customer.customerId} linked for this browser session.`);
      navigate(`/customer/${customer.customerId}`);
    } catch (requestError) {
      const mapped = mapAxiosError(requestError);

      if (mapped.code === 'UNAUTHORISED' || mapped.code === 'UNAUTHORIZED' || mapped.code === 'CUSTOMER_NOT_FOUND') {
        setLinkError({
          ...mapped,
          message: 'That customer profile is not available for the logged-in user. Check the customer ID and try again.'
        });
      } else {
        setLinkError(mapped);
      }
    } finally {
      setIsLinkingCustomer(false);
    }
  }

  return (
    <div className="stack home-page">
      <section className="stack offers-panel">
        <div className="offers-header">
          <div>
            <span className="kicker">Featured offers</span>
            <h3>Slide through this month&apos;s banking offers</h3>
          </div>
        </div>
        <div className="offers-slider-window">
          <div
            className="offers-slider-track"
            style={{ transform: `translateX(-${activeOfferIndex * 100}%)` }}
          >
            {offers.map((offer) => (
              <article key={offer.title} className="offer-slide" role="tabpanel">
                <div className="offer-copy">
                  <p className="offer-badge">{offer.badge}</p>
                  <h4>{offer.title}</h4>
                  <p className="muted">{offer.description}</p>
                </div>
                <div className="offer-media">
                  <img src={offer.image} alt={offer.title} />
                </div>
              </article>
            ))}
          </div>
        </div>
        <div className="offers-controls bottom" role="tablist" aria-label="Offer slides">
          {offers.map((offer, index) => (
            <button
              key={offer.title}
              type="button"
              className={index === activeOfferIndex ? 'offer-dot active' : 'offer-dot'}
              onClick={() => { setActiveOfferIndex(index); startSliderTimer(); }}
              aria-label={`Show offer ${index + 1}`}
              aria-selected={index === activeOfferIndex}
              role="tab"
            />
          ))}
        </div>
      </section>

      {!isAuthenticated ? (
        <Link className="big-get-started" to="/register">Register</Link>
      ) : (
        <section className="panel stack">
          <h3>Quick Navigation</h3>
          {isAdmin ? (
            <>
            </>
          ) : (
            <>
            </>
          )}
          <div className="actions">
            {isAdmin ? <button type="button" onClick={openCustomer} disabled={!lookup.customerId}>Open Customer</button> : null}
            {isAdmin ? <button type="button" onClick={openAccount} disabled={!lookup.accountId}>Open Account</button> : null}
            {authState.customerId ? <Link className="button-link" to={`/customer/${authState.customerId}`}>My Profile</Link> : null}
            {authState.customerId ? <Link className="button-link subtle" to={`/customer/${authState.customerId}/accounts`}>My Accounts</Link> : null}
            {!isAdmin && !authState.customerId ? <button type="button" onClick={linkCustomerProfile} disabled={!lookup.customerId || isLinkingCustomer}>Link My Profile</button> : null}
            {!authState.customerId ? <Link className="button-link subtle" to="/customer/create">Create Customer</Link> : null}
          </div>
        </section>
      )}

      <footer className="overview-footer">
        <div className="footer-bottom-row">
          <p className="footer-meta">© {currentYear} Voltio Platform</p>
          <div className="footer-center-info">
            <p className="footer-meta">VoltioSupport@VoltioBank.ca</p>
            <p className="footer-meta">Mon-Fri, 8:00 AM to 6:00 PM</p>
          </div>
          <div className="footer-social" aria-label="Social media links">
            <a href="#" aria-label="Facebook">f</a>
            <a href="#" aria-label="Instagram">ig</a>
            <a href="#" aria-label="X">x</a>
            <a href="#" aria-label="LinkedIn">in</a>
            <a href="#" aria-label="YouTube">yt</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
