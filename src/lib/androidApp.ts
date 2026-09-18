// True when running inside the JustQuran Android WebView shell.
// MainActivity appends " JustQuranAndroidApp" to the WebView user agent, so
// web code can hide browser-only UI (install prompts, web credits, Ko-fi).
export const isAndroidApp =
  typeof navigator !== 'undefined' && navigator.userAgent.includes('JustQuranAndroidApp');
