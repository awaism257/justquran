# JustQuran

**Free · No ads · No sign-in · No tracking · Fully offline**

JustQuran is a trilingual Quran reader — **Arabic · Urdu · English** — built as an
installable web app (PWA) for Android, iOS and desktop, with a native Android app
(Kotlin) sharing the same design family.

**Live app:** https://justquran-app.netlify.app
**Android package id:** `org.justquran.app`

---

## Features

- **Verse-by-verse cards** — Arabic with transliteration and translations, per-verse
  audio, bookmark, and copy/share
- **Continuous folio (mushaf-style)** — flowing Arabic-only page with gold verse
  medallions
- **Waqaf (stop) marks** — the pause signs from the Indopak mushaf tradition,
  rendered above each verse marker (single, double and triple mark clusters)
- **Recitation** — Sheikh Mahmoud Khalil Al-Hussary (Murattal), whole-surah playback
  with auto-scroll that follows the recitation verse by verse (and politely pauses
  itself when you scroll manually), plus per-verse play and optional offline download
- **Reading comfort** — independent font-size sliders for Arabic / English / Urdu /
  transliteration, light & dark themes, cream "mushaf paper" option
- **Search, bookmarks, juz index, surah index, last-read position**
- **Installable PWA** — add to home screen; works fully offline after first load
  (service-worker cache; optional full audio download for offline recitation)

## Tech stack

- **React 19 + TypeScript + Vite 7**, Tailwind CSS 3, shadcn/ui, lucide-react icons
- **PWA**: hand-written service worker (`public/sw.js`), manifest, offline-first caching
- **Audio**: per-verse MP3s streamed from everyayah.com, optionally cached in
  Cache Storage for offline use
- No backend, no analytics, no accounts — everything lives in the browser's
  localStorage / Cache Storage

## Build & run

Requires Node.js 20+.

```bash
npm install
npm run dev      # local dev server
npm run build    # production build → dist/
```

Deploy the contents of `dist/` to any static host (the app is deployed on Netlify).
`public/_headers` and `public/_redirects` are Netlify conventions; on other hosts,
configure the SPA fallback (`/* → /index.html`) yourself.

> **Note for contributors:** after changing anything in `public/`, bump
> `CACHE_VERSION` at the top of `public/sw.js`, otherwise installed copies of the
> app keep serving stale files.

## Project layout

```
public/
  data/quran-bundle.json   # the full text bundle (see ATTRIBUTION.md)
  fonts/                   # Amiri Quran, Noto Nastaliq Urdu, DigitalKhatt IndoPak (OFL)
  icons/                   # PWA icons
  sw.js                    # service worker (bump CACHE_VERSION on every change!)
src/
  pages/                   # Home, Reading, Search, Bookmarks, Settings, Recitation, …
  components/              # VerseCard, FolioPage, SurahPlayer, InstallPrompt, …
  lib/                     # data, audio, bookmarks, settings, share helpers
```

## Texts, fonts, audio — licences

Everything bundled in this app is freely redistributable, public domain, or used
with permission. **Full details and credits: [ATTRIBUTION.md](ATTRIBUTION.md).**

| Content | Source | Licence |
| --- | --- | --- |
| Arabic text | Digital Khatt Indopak script — QUL / Tarteel Inc | used with permission (see ATTRIBUTION.md) |
| English translation | Talal Itani, ClearQuran.com | CC BY-ND 4.0 (bundled unmodified) |
| Urdu translation | Fateh Muhammad Jalandhari | public domain; digitised by this project |
| Transliteration | Tanzil Project | Tanzil licence (see ATTRIBUTION.md) |
| Recitation audio | everyayah.com (Al-Hussary, Murattal) | streamed, not bundled |
| Fonts | Amiri Quran · Noto Nastaliq Urdu · DigitalKhatt IndoPak | SIL OFL 1.1 |

## Licence

The **app source code** is released under the [MIT License](LICENSE).
The **bundled texts, fonts and audio** remain under their own licences as listed
in [ATTRIBUTION.md](ATTRIBUTION.md) — please respect them if you reuse the data.

## Contributing

Issues and pull requests are welcome. The project is deliberately simple: no
accounts, no tracking, no ads, no paywalls — it will stay that way.
Please note that this project follows a
[Code of Conduct](CODE_OF_CONDUCT.md).

---

This site is powered by [Netlify](https://www.netlify.com) — free Open Source plan.
