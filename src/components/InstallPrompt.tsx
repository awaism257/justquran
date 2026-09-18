// Install sheet: one friendly prompt that helps mobile visitors add JustQuran
// to their home screen. Android gets a one-tap native install button (via the
// browser's beforeinstallprompt event); iPhone/iPad get step-by-step
// Share → Add to Home Screen instructions (Apple allows no programmatic prompt).
// Auto-shows once (snoozed 30 days on "Not now"); can always be reopened from
// Settings → "Install app" which dispatches the 'jq:show-install' event.

import { useEffect, useState } from 'react';
import { Download, X } from 'lucide-react';
import { isAndroidApp } from '@/lib/androidApp';

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

const DISMISS_KEY = 'jq-install-dismissed';
const SNOOZE_MS = 30 * 24 * 60 * 60 * 1000; // 30 days

let deferredPrompt: BeforeInstallPromptEvent | null = null;
let installed = false;

// Capture the native prompt as early as possible (module load).
window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e as BeforeInstallPromptEvent;
});
window.addEventListener('appinstalled', () => {
  installed = true;
  deferredPrompt = null;
  try { localStorage.setItem(DISMISS_KEY, 'installed'); } catch { /* private mode */ }
});

function isStandalone(): boolean {
  return (
    window.matchMedia?.('(display-mode: standalone)').matches ||
    window.matchMedia?.('(display-mode: minimal-ui)').matches ||
    (navigator as unknown as { standalone?: boolean }).standalone === true
  );
}

function platform(): 'ios' | 'android' | 'other' {
  const ua = navigator.userAgent;
  if (/iphone|ipad|ipod/i.test(ua)) return 'ios';
  if (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1) return 'ios'; // iPadOS desktop UA
  if (/android/i.test(ua)) return 'android';
  return 'other';
}

function snoozed(): boolean {
  try {
    const d = localStorage.getItem(DISMISS_KEY);
    if (d === 'installed') return true;
    if (d && Date.now() - Number(d) < SNOOZE_MS) return true;
  } catch { /* private mode */ }
  return false;
}

export default function InstallPrompt() {
  // Inside the native Android app there is nothing to install — never show.
  if (isAndroidApp) return null;
  return <InstallPromptInner />;
}

function InstallPromptInner() {
  const [visible, setVisible] = useState(false);
  const [hasNative, setHasNative] = useState(false);
  const pf = platform();

  useEffect(() => {
    const show = () => {
      if (isStandalone() || installed) return;
      setHasNative(deferredPrompt !== null);
      setVisible(true);
    };
    window.addEventListener('jq:show-install', show);

    let timer: number | undefined;
    if (pf !== 'other' && !isStandalone() && !installed && !snoozed()) {
      // First visit on a phone: suggest installing after a short settle-in delay.
      timer = window.setTimeout(show, 6000);
    }
    return () => {
      window.removeEventListener('jq:show-install', show);
      if (timer) window.clearTimeout(timer);
    };
  }, [pf]);

  if (!visible || isStandalone()) return null;

  const dismiss = () => {
    try { localStorage.setItem(DISMISS_KEY, String(Date.now())); } catch { /* private mode */ }
    setVisible(false);
  };

  const installNow = async () => {
    if (!deferredPrompt) return;
    await deferredPrompt.prompt();
    const choice = await deferredPrompt.userChoice;
    if (choice.outcome === 'accepted') {
      installed = true;
      try { localStorage.setItem(DISMISS_KEY, 'installed'); } catch { /* private mode */ }
      setVisible(false);
    }
    deferredPrompt = null;
  };

  return (
    <div className="install-backdrop" onClick={dismiss}>
      <div
        className="install-sheet"
        role="dialog"
        aria-modal="true"
        aria-label="Install JustQuran"
        onClick={(e) => e.stopPropagation()}
      >
        <button className="icon-btn install-x" aria-label="Close" onClick={dismiss}>
          <X size={16} />
        </button>
        <div className="install-title">Install JustQuran</div>
        <p className="install-sub">Free forever · no account · no ads · works fully offline</p>

        {pf === 'android' && hasNative ? (
          <button className="install-primary" onClick={installNow}>
            <Download size={17} /> Install app
          </button>
        ) : pf === 'ios' ? (
          <ol className="install-steps">
            <li>Tap the <b>Share</b> button — the square with an arrow pointing up (bottom of Safari)</li>
            <li>Scroll down and tap <b>Add to Home Screen</b></li>
            <li>Tap <b>Add</b> (top right)</li>
          </ol>
        ) : (
          <ol className="install-steps">
            <li>Tap the <b>⋮ menu</b> (top right of your browser)</li>
            <li>Tap <b>Add to Home screen</b> or <b>Install app</b></li>
            <li>Tap <b>Install</b></li>
          </ol>
        )}

        <button className="install-later" onClick={dismiss}>Not now</button>
      </div>
    </div>
  );
}
