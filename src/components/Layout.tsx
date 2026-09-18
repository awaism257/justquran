import type { ReactNode } from 'react';

/** Centred mobile shell: max-width 560px. */
export default function Layout({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto w-full min-h-[100dvh]" style={{ maxWidth: 560 }}>
      {children}
    </div>
  );
}
