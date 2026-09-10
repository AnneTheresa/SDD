import axios from "axios";
import { readStoredAuthState } from "../auth/authState";

const AUTH_STORAGE_KEY = "banking-app-auth";
const CUSTOMER_CONTEXT_KEY = "banking-app-customer-contexts";

function isAuthEndpoint(url) {
  if (!url || typeof url !== "string") {
    return false;
  }

  return /\/(api\/)?auth\/(login|register|refresh)(\/|$)/.test(url);
}

function attachSessionExpiredInterceptor(client) {
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      const status = error?.response?.status;
      const requestUrl = error?.config?.url || "";
      const authState = readStoredAuthState();
      const hadSession = Boolean(authState.accessToken);
      // Check that the token used for this specific request is still the current one.
      // If it differs, this is a stale in-flight request from a previous session
      // (e.g., user logged out and a new user logged in before the old request completed).
      // Clearing the new user's session in that case would be wrong.
      const sentToken = (error?.config?.headers?.Authorization || "").replace(
        /^Bearer\s+/,
        "",
      );
      const tokenStillCurrent = sentToken === authState.accessToken;
      // Only clear the session on 401 (Unauthorized = token missing/expired/invalid).
      // 403 (Forbidden) means the user IS authenticated but lacks permission — do not log them out.
      // Ignore auth endpoint failures during login/register so user sees the real error.
      if (
        status === 401 &&
        hadSession &&
        tokenStillCurrent &&
        !isAuthEndpoint(requestUrl)
      ) {
        window.localStorage.removeItem(AUTH_STORAGE_KEY);
        window.localStorage.removeItem(CUSTOMER_CONTEXT_KEY);
        // Only redirect if not already on the login page to avoid redirect loops
        if (!window.location.pathname.startsWith("/login")) {
          window.location.replace("/login?error=session_expired");
        }
      }
      return Promise.reject(error);
    },
  );
  return client;
}

const mergedBackendBaseUrl =
  import.meta.env.VITE_GROUP123_BACKEND_BASE_URL ||
  import.meta.env.VITE_BANKING_API_BASE_URL ||
  import.meta.env.VITE_LOGIN_API_BASE_URL ||
  import.meta.env.VITE_ACCOUNT_SERVICE_BASE_URL ||
  (import.meta.env.DEV ? "/" : "/");

function attachAuthInterceptor(client) {
  client.interceptors.request.use((config) => {
    const authState = readStoredAuthState();
    const headers = config.headers ?? {};
    const tokenFresh =
      authState.accessToken &&
      (!authState.expiresAt || authState.expiresAt > Date.now());

    if (tokenFresh) {
      headers.Authorization = `Bearer ${authState.accessToken}`;
    }

    config.headers = headers;
    return config;
  });

  return client;
}

export const loginApiClient = attachSessionExpiredInterceptor(
  attachAuthInterceptor(
    axios.create({
      baseURL: mergedBackendBaseUrl,
    }),
  ),
);

export const accountApiClient = attachSessionExpiredInterceptor(
  attachAuthInterceptor(
    axios.create({
      baseURL: mergedBackendBaseUrl,
    }),
  ),
);

function firstValidationError(errors) {
  if (!Array.isArray(errors) || errors.length === 0) {
    return null;
  }

  const [firstError] = errors;
  if (typeof firstError === "string") {
    return firstError;
  }

  return firstError.defaultMessage || firstError.message || null;
}

export function mapAxiosError(error) {
  const response = error?.response;
  const data = response?.data;
  const validationMessage = firstValidationError(data?.errors);

  if (data?.code === "ACCOUNT_TEMPORARILY_RESTRICTED") {
    return {
      code: data.code,
      message: "This account is temporarily restricted. Please contact support.",
      field: data.field ?? null,
    };
  }

  if (data?.code || data?.message) {
    return {
      code: data.code || `HTTP_${response?.status || "UNKNOWN"}`,
      message:
        data.message ||
        validationMessage ||
        `Request failed with status ${response?.status}`,
      field: data.field ?? null,
    };
  }

  if (validationMessage) {
    return {
      code: `HTTP_${response?.status || 422}`,
      message: validationMessage,
      field: null,
    };
  }

  if (typeof data === "string" && data.trim().length > 0) {
    return {
      code: `HTTP_${response?.status || "UNKNOWN"}`,
      message: data,
      field: null,
    };
  }

  return {
    code: "UNKNOWN_ERROR",
    message: response?.status
      ? `Request failed with status ${response.status}`
      : "Request failed",
    field: null,
  };
}

/**
 * Map Savings Goal backend error codes to user-facing messages
 * All 9 error codes from Savings Goal feature mapped here (Single Source of Truth)
 */
export function mapSavingsGoalErrorCode(errorCode) {
  const errorMapping = {
    // 400: Validation errors
    INVALID_TARGET_AMOUNT: "Target amount must be greater than $0",
    INVALID_TARGET_DATE: "Target date must be today or in the future",
    INVALID_GOAL_NAME: "Please enter a goal name",
    MISSING_REQUIRED_FIELD: "All fields are required",

    // 403: Authorization errors
    UNAUTHORIZED_ACCOUNT_ACCESS:
      "You do not have permission to access this account",

    // 404: Not found errors
    ACCOUNT_NOT_FOUND: "Account not found or is inactive",
    GOAL_NOT_FOUND: "Savings goal not found",

    // 409: Conflict errors
    GOAL_ALREADY_EXISTS: "An active goal already exists for this account",

    // 500: Server errors
    INTERNAL_SERVER_ERROR: "Something went wrong. Please try again.",
  };

  return errorMapping[errorCode] || `Error: ${errorCode}`;
}

/**
 * Enhanced error mapping specifically for Savings Goal endpoints
 * Extends mapAxiosError with user-facing messages
 */
export function mapSavingsGoalAxiosError(error) {
  const baseError = mapAxiosError(error);

  // If we have a Savings Goal error code, map it to user-facing message
  if (
    baseError.code &&
    baseError.code in
      {
        INVALID_TARGET_AMOUNT: true,
        INVALID_TARGET_DATE: true,
        INVALID_GOAL_NAME: true,
        MISSING_REQUIRED_FIELD: true,
        UNAUTHORIZED_ACCOUNT_ACCESS: true,
        ACCOUNT_NOT_FOUND: true,
        GOAL_NOT_FOUND: true,
        GOAL_ALREADY_EXISTS: true,
        INTERNAL_SERVER_ERROR: true,
      }
  ) {
    return {
      ...baseError,
      message: mapSavingsGoalErrorCode(baseError.code),
    };
  }

  return baseError;
}
