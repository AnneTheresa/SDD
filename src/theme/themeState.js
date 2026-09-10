export const THEME_STORAGE_KEY = 'banking-app-theme';
export const DEFAULT_THEME = 'new';

export function normalizeTheme(theme) {
  return theme === 'classic' ? 'classic' : DEFAULT_THEME;
}

function getLocalStorage() {
  if (typeof window === 'undefined') {
    return null;
  }

  try {
    return window.localStorage ?? null;
  } catch {
    return null;
  }
}

export function readStoredTheme() {
  const storage = getLocalStorage();

  if (!storage) {
    return DEFAULT_THEME;
  }

  return normalizeTheme(storage.getItem(THEME_STORAGE_KEY));
}

export function writeStoredTheme(theme) {
  const storage = getLocalStorage();

  if (!storage) {
    return;
  }

  storage.setItem(THEME_STORAGE_KEY, normalizeTheme(theme));
}
