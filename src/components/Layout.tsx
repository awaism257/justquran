import type { ReactNode } from 'react';
import DesktopSidebar from '@/components/DesktopSidebar';

/**
 * Responsive Application Shell:
 * - On Mobile (< 1024px): Preserves the focused 560px mobile container.
 * - On Desktop (>= 1024px): Seamlessly expands into a persistent desktop sidebar
 *   and a centered high-comfort reading/browsing canvas (up to 880px wide),
 *   modeled after the Munajaat Maqbool desktop experience.
 */
export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="min-h-[100dvh] w-full flex justify-center bg-[var(--bg)] text-[var(--text)]">
      {/* Desktop Sidebar (hidden on mobile, visible on desktop >= 1024px) */}
      <DesktopSidebar />

      {/* Main Content Area */}
      <main className="flex-1 min-w-0 w-full flex justify-center">
        <div className="w-full max-w-[560px] lg:max-w-[880px] min-h-[100dvh] flex flex-col transition-[max-width] duration-200">
          {children}
        </div>
      </main>
    </div>
  );
}
