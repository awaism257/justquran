import { ArrowLeft, Home } from 'lucide-react';
import type { ReactNode } from 'react';
import { useNavigate } from 'react-router';

interface BackBarProps {
  title: string;
  meta?: string;
  onBack?: () => void;
  /** Show a Home button (→ main menu) instead of Back. */
  home?: boolean;
  /** Optional extra buttons rendered at the far right of the bar. */
  actions?: ReactNode;
  /** Optional inline style for the title (e.g. RTL + Nastaliq for Urdu book headings). */
  titleStyle?: React.CSSProperties;
}

/** Sub-screen header: ← back (or ⌂ home), title, right meta, optional actions, green divider below. */
export default function BackBar({ title, meta, onBack, home, actions, titleStyle }: BackBarProps) {
  const navigate = useNavigate();
  return (
    <header className="sticky top-0 z-40" style={{ background: 'var(--chrome)' }}>
      <div className="backbar">
        <button
          className="icon-btn"
          aria-label={home ? 'Home' : 'Back'}
          onClick={() => (home ? navigate('/') : onBack ? onBack() : navigate(-1))}
        >
          {home ? <Home size={18} /> : <ArrowLeft size={18} />}
        </button>
        <div className={`backbar-content ${meta ? 'has-meta' : ''}`}>
          <div className="backbar-title" style={titleStyle}>
            {title}
          </div>
          {meta ? <div className="backbar-meta">{meta}</div> : null}
        </div>
        {actions}
      </div>
      <div className="greendiv" />
    </header>
  );
}
