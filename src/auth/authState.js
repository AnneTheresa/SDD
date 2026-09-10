const STORAGE_KEY = 'banking-app-auth';
const CUSTOMER_CONTEXT_KEY = 'banking-app-customer-contexts';

export const emptyAuthState = {
  accessToken: '',
  refreshToken: '',
  tokenType: 'Bearer',
  expiresIn: null,
  expiresAt: null,
  username: '',
  subject: '',
  roles: [],
  customerId: ''
};

function normalizeCustomerId(customerId) {
  if (customerId == null) {
    return '';
  }

  return String(customerId);
}

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  return window.atob(padded);
}

export function decodeJwt(token) {
  if (!token || !token.includes('.')) {
    return {};
  }

  try {
    const [, payload] = token.split('.');
    return JSON.parse(decodeBase64Url(payload));
  } catch {
    return {};
  }
}

function normalizeRoles(roles) {
  if (Array.isArray(roles)) {
    return roles.filter(Boolean);
  }

  if (typeof roles === 'string' && roles.trim().length > 0) {
    return roles.split(',').map((value) => value.trim()).filter(Boolean);
  }

  return [];
}

export function readStoredAuthState() {
  const saved = window.localStorage.getItem(STORAGE_KEY);
  if (!saved) {
    return emptyAuthState;
  }

  try {
    const parsed = JSON.parse(saved);
    const state = {
      ...emptyAuthState,
      ...parsed,
      roles: normalizeRoles(parsed.roles)
    };

    // If we know the token's expiry and it has passed, discard it now so that
    // ProtectedRoute redirects cleanly to /login instead of firing API calls
    // that all return 401 and trigger the "session expired" banner.
    if (state.expiresAt && state.expiresAt < Date.now()) {
      window.localStorage.removeItem(STORAGE_KEY);
      return emptyAuthState;
    }

    return state;
  } catch {
    return emptyAuthState;
  }
}

function readStoredCustomerContexts() {
  const saved = window.localStorage.getItem(CUSTOMER_CONTEXT_KEY);
  if (!saved) {
    return {};
  }

  try {
    const parsed = JSON.parse(saved);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return {};
    }

    return Object.fromEntries(
      Object.entries(parsed)
        .filter(([subject, customerId]) => Boolean(subject) && typeof customerId === 'string' && customerId.length > 0)
    );
  } catch {
    return {};
  }
}

function writeStoredCustomerContexts(contexts) {
  const entries = Object.entries(contexts).filter(([subject, customerId]) => Boolean(subject) && Boolean(customerId));

  if (entries.length === 0) {
    window.localStorage.removeItem(CUSTOMER_CONTEXT_KEY);
    return;
  }

  window.localStorage.setItem(CUSTOMER_CONTEXT_KEY, JSON.stringify(Object.fromEntries(entries)));
}

export function rememberCustomerContext(subject, customerId) {
  if (!subject) {
    return;
  }

  const normalizedCustomerId = normalizeCustomerId(customerId);
  if (!normalizedCustomerId) {
    return;
  }

  const contexts = readStoredCustomerContexts();
  contexts[subject] = normalizedCustomerId;
  writeStoredCustomerContexts(contexts);
}

export function clearRememberedCustomerContext(subject) {
  if (!subject) {
    return;
  }

  const contexts = readStoredCustomerContexts();
  delete contexts[subject];
  writeStoredCustomerContexts(contexts);
}

export function getRememberedCustomerContext(subject) {
  if (!subject) {
    return '';
  }

  return normalizeCustomerId(readStoredCustomerContexts()[subject]);
}

export function writeStoredAuthState(state) {
  if (!state.accessToken) {
    window.localStorage.removeItem(STORAGE_KEY);
    return;
  }

  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

export function buildAuthenticatedState(authResponse, username) {
  const existingState = readStoredAuthState();
  const decoded = decodeJwt(authResponse.accessToken);
  const subject = decoded.sub || existingState.subject || '';
  const roles = normalizeRoles(decoded.roles || existingState.roles);
  const expiresIn = Number(authResponse.expiresIn || 0) || null;
  const expiresAt = expiresIn ? Date.now() + expiresIn * 1000 : null;
  const canReuseCustomerContext = existingState.subject && existingState.subject === subject;
  const rememberedCustomerId = getRememberedCustomerContext(subject);
  // Also try to read customerId from JWT claims (backend may embed it)
  const jwtCustomerId = normalizeCustomerId(
    decoded.customerId || decoded.customer_id || decoded.cid || ''
  );

  return {
    accessToken: authResponse.accessToken,
    refreshToken: authResponse.refreshToken || '',
    tokenType: authResponse.tokenType || 'Bearer',
    expiresIn,
    expiresAt,
    username,
    subject,
    roles,
    customerId: rememberedCustomerId || jwtCustomerId || (canReuseCustomerContext ? normalizeCustomerId(existingState.customerId) : '')
  };
}