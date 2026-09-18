import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';
import type { ReactNode } from 'react';

export type ThemeChoice = 'system' | 'light' | 'dark';

export interface FontScales {
  ar: number; // 70–160 (percent)
  en: number;
  ur: number;
  tr: number;
}

export interface Settings {
  theme: ThemeChoice;
  fonts: FontScales;
  showEn: boolean;
  showUr: boolean;
  showTr: boolean;
  verseByVerse: boolean;
  alwaysCreamPage: boolean;
  audioAutoAdvance: boolean;
}

export const DEFAULT_SETTINGS: Settings = {
  theme: 'system',
  fonts: { ar: 100, en: 100, ur: 100, tr: 100 },
  showEn: true,
  showUr: true,
  showTr: true,
  verseByVerse: true,
  alwaysCreamPage: false,
  audioAutoAdvance: false,
};

const LS_KEY = 'jq-settings';

function loadSettings(): Settings {
  try {
    const raw = localStorage.getItem(LS_KEY);
    if (!raw) return DEFAULT_SETTINGS;
    const parsed = JSON.parse(raw);
    return {
      ...DEFAULT_SETTINGS,
      ...parsed,
      fonts: { ...DEFAULT_SETTINGS.fonts, ...(parsed.fonts ?? {}) },
    };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

function clamp(v: number) {
  return Math.min(160, Math.max(70, Math.round(v)));
}



interface SettingsCtx {
  settings: Settings;
  resolvedTheme: 'light' | 'dark';
  setTheme: (t: ThemeChoice) => void;
  setFontScale: (key: keyof FontScales, pct: number) => void;
  toggle: (key: 'showEn' | 'showUr' | 'showTr' | 'verseByVerse' | 'alwaysCreamPage' | 'audioAutoAdvance') => void;
  set: (patch: Partial<Settings>) => void;
}

const Ctx = createContext<SettingsCtx | null>(null);

export function SettingsProvider({ children }: { children: ReactNode }) {
  const [settings, setSettings] = useState<Settings>(loadSettings);
  const [systemDark, setSystemDark] = useState<boolean>(
    () => window.matchMedia('(prefers-color-scheme: dark)').matches,
  );

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const on = (e: MediaQueryListEvent) => setSystemDark(e.matches);
    mq.addEventListener('change', on);
    return () => mq.removeEventListener('change', on);
  }, []);

  const resolvedTheme: 'light' | 'dark' =
    settings.theme === 'system' ? (systemDark ? 'dark' : 'light') : settings.theme;

  // Apply theme class + font scale CSS vars to <html>
  useEffect(() => {
    const el = document.documentElement;
    el.classList.toggle('light', resolvedTheme === 'light');
    el.style.setProperty('--fs-ar', `${clamp(settings.fonts.ar)/100}`);
    el.style.setProperty('--fs-en', `${clamp(settings.fonts.en)/100}`);
    el.style.setProperty('--fs-ur', `${clamp(settings.fonts.ur)/100}`);
    el.style.setProperty('--fs-tr', `${clamp(settings.fonts.tr)/100}`);
  }, [resolvedTheme, settings.fonts]);

  useEffect(() => {
    try {
      localStorage.setItem(LS_KEY, JSON.stringify(settings));
    } catch {
      /* storage unavailable */
    }
  }, [settings]);

  const set = useCallback((patch: Partial<Settings>) => {
    setSettings((s) => ({ ...s, ...patch }));
  }, []);
  const setTheme = useCallback((t: ThemeChoice) => set({ theme: t }), [set]);
  const setFontScale = useCallback(
    (key: keyof FontScales, pct: number) =>
      setSettings((s) => ({ ...s, fonts: { ...s.fonts, [key]: clamp(pct) } })),
    [],
  );
  const toggle = useCallback(
    (key: 'showEn' | 'showUr' | 'showTr' | 'verseByVerse' | 'alwaysCreamPage' | 'audioAutoAdvance') =>
      setSettings((s) => ({ ...s, [key]: !s[key] })),
    [],
  );

  const value = useMemo(
    () => ({ settings, resolvedTheme, setTheme, setFontScale, toggle, set }),
    [settings, resolvedTheme, setTheme, setFontScale, toggle, set],
  );

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useSettings(): SettingsCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useSettings must be used inside <SettingsProvider>');
  return ctx;
}
