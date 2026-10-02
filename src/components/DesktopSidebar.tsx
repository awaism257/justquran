import { Link, useLocation } from 'react-router';
import {
  BookMarked,
  BookOpen,
  CircleHelp,
  HandHeart,
  Headphones,
  Info,
  Library,
  Moon,
  Rows3,
  Search,
  Settings,
  Sun,
} from 'lucide-react';
import { useSettings } from '@/lib/settings';
import { isAndroidApp } from '@/lib/androidApp';

interface NavItemProps {
  to: string;
  icon: React.ReactNode;
  label: string;
  badge?: string;
}

function NavItem({ to, icon, label, badge }: NavItemProps) {
  const location = useLocation();
  const isActive =
    to === '/'
      ? location.pathname === '/'
      : location.pathname === to || location.pathname.startsWith(`${to}/`);

  return (
    <Link
      to={to}
      className={`group flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-sm transition-all duration-150 ${
        isActive
          ? 'bg-[var(--green)] text-black font-semibold shadow-sm'
          : 'text-[var(--text)] opacity-85 hover:opacity-100 hover:bg-[var(--line)]'
      }`}
    >
      <span className={`shrink-0 transition-transform duration-150 group-hover:scale-105 ${isActive ? 'text-black' : 'text-[var(--green)]'}`}>
        {icon}
      </span>
      <span className="flex-1 truncate">{label}</span>
      {badge && (
        <span
          className={`text-[11px] px-1.5 py-0.5 rounded-md font-mono ${
            isActive
              ? 'bg-black/20 text-black'
              : 'bg-[var(--line)] text-[var(--muted)]'
          }`}
        >
          {badge}
        </span>
      )}
    </Link>
  );
}

export default function DesktopSidebar() {
  const { resolvedTheme, set } = useSettings();

  const toggleTheme = () => {
    set({ theme: resolvedTheme === 'dark' ? 'light' : 'dark' });
  };

  return (
    <aside
      className="hidden lg:flex flex-col w-72 shrink-0 sticky top-0 h-screen overflow-y-auto border-r border-[var(--line)] bg-[var(--chrome)] select-none z-30"
      style={{ scrollbarWidth: 'thin' }}
    >
      {/* Brand Header */}
      <div className="p-5 pb-4 border-b border-[var(--line)]">
        <Link to="/" className="flex items-center gap-3 group text-decoration-none">
          <img
            src="/icons/icon-192.png"
            alt="JustQuran Logo"
            className="w-10 h-10 rounded-xl shadow-md transition-transform duration-200 group-hover:scale-105 shrink-0"
          />
          <div className="min-w-0">
            <h1 className="font-serif text-lg font-bold tracking-tight text-[var(--text)] leading-tight">
              JustQuran
            </h1>
            <p className="text-xs text-[var(--muted)] truncate mt-0.5">
              The Noble Quran · Arabic & Translation
            </p>
          </div>
        </Link>
      </div>

      {/* Navigation Sections */}
      <div className="flex-1 px-3 py-4 space-y-6">
        {/* Primary Modes */}
        <div>
          <div className="px-3 pb-2 text-[10.5px] font-semibold tracking-wider uppercase text-[var(--green)] opacity-85">
            Reading Modes
          </div>
          <div className="space-y-1">
            <NavItem to="/book" icon={<Library size={18} />} label="Paged Mushaf" badge="Pages" />
            <NavItem to="/surahs" icon={<BookOpen size={18} />} label="Verse by Verse" badge="114" />
            <NavItem to="/recitation" icon={<Headphones size={18} />} label="Recitation" badge="Audio" />
            <NavItem to="/khatm" icon={<HandHeart size={18} />} label="Khatm Dua" />
          </div>
        </div>

        {/* Index & Browse */}
        <div>
          <div className="px-3 pb-2 text-[10.5px] font-semibold tracking-wider uppercase text-[var(--green)] opacity-85">
            Browse
          </div>
          <div className="space-y-1">
            <NavItem to="/surahs" icon={<BookOpen size={18} />} label="All Surahs" />
            <NavItem to="/juz" icon={<Rows3 size={18} />} label="Juz Index" badge="30" />
            <NavItem to="/search" icon={<Search size={18} />} label="Search" />
            <NavItem to="/bookmarks" icon={<BookMarked size={18} />} label="Bookmarks" />
          </div>
        </div>

        {/* Preferences & Help */}
        <div>
          <div className="px-3 pb-2 text-[10.5px] font-semibold tracking-wider uppercase text-[var(--green)] opacity-85">
            Preferences
          </div>
          <div className="space-y-1">
            <NavItem to="/settings" icon={<Settings size={18} />} label="Settings" />
            <NavItem to="/help" icon={<CircleHelp size={18} />} label="How to use" />
            <NavItem to="/about" icon={<Info size={18} />} label="About JustQuran" />
          </div>
        </div>
      </div>

      {/* Sidebar Footer */}
      <div className="p-4 border-t border-[var(--line)] bg-[rgba(0,0,0,0.12)] flex flex-col gap-3">
        {/* Theme Toggle Button */}
        <button
          onClick={toggleTheme}
          className="flex items-center justify-between w-full px-3 py-2 rounded-xl text-xs font-medium text-[var(--text)] bg-[rgba(255,255,255,0.05)] hover:bg-[rgba(255,255,255,0.1)] transition-colors border border-[var(--line)]"
          aria-label="Toggle Theme"
        >
          <span className="flex items-center gap-2">
            {resolvedTheme === 'dark' ? (
              <Moon size={15} className="text-[var(--green)]" />
            ) : (
              <Sun size={15} className="text-[var(--green)]" />
            )}
            <span>Theme</span>
          </span>
          <span className="capitalize text-[11px] text-[var(--muted)] font-mono">
            {resolvedTheme}
          </span>
        </button>

        {/* Attribution & Status */}
        <div className="text-[11px] text-[var(--muted)] text-center space-y-1">
          <p>Fully offline · No ads · No tracking</p>
          {!isAndroidApp && (
            <p>
              Powered by{' '}
              <a
                href="https://www.netlify.com"
                target="_blank"
                rel="noopener noreferrer"
                className="text-[var(--green)] hover:underline"
              >
                Netlify
              </a>
            </p>
          )}
        </div>
      </div>
    </aside>
  );
}
