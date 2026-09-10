import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { DEFAULT_THEME, readStoredTheme, writeStoredTheme } from './themeState';

const ThemeContext = createContext(null);

function applyThemeClass(theme) {
  if (typeof document === 'undefined') {
    return;
  }

  const body = document.body;
  body.classList.remove('theme-new', 'theme-classic');
  body.classList.add(theme === 'classic' ? 'theme-classic' : 'theme-new');
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(readStoredTheme);

  useEffect(() => {
    writeStoredTheme(theme);
    applyThemeClass(theme);
  }, [theme]);

  const value = useMemo(() => ({
    theme,
    isClassic: theme === 'classic',
    setTheme,
    toggleTheme() {
      setTheme((current) => (current === 'classic' ? DEFAULT_THEME : 'classic'));
    }
  }), [theme]);

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const context = useContext(ThemeContext);

  if (!context) {
    throw new Error('useTheme must be used inside ThemeProvider');
  }

  return context;
}
