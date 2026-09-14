# Attribution & Licences

JustQuran bundles texts and fonts from several sources. Each remains under its
own licence; the MIT License in `LICENSE` applies to the JustQuran source code
only. This file is the canonical credit list — the same credits appear in the
app under **Settings → About & credits**.

---

## 1. Arabic Quranic text

**Digital Khatt IndoPak script — QUL / Tarteel Inc**
Source: https://qul.tarteel.ai/resources/quran-script/566 (ayah-by-ayah JSON)
The text is bundled **verbatim**, including the ayah-end waqaf (stop) marks used
in the Indopak mushaf tradition, which the app renders above each verse marker.
Digital Khatt typeface by Amine Anane / Tarteel Inc.
Redistribution in this app is used with permission from Tarteel Inc
(contact: support@tarteel.ai). If you reuse this data, please credit
**QUL / Tarteel Inc** and keep the text unmodified.

## 2. English translation

**The Clear Quran by Dr. Talal Itani — ClearQuran.com**
Licence: **Creative Commons Attribution–NoDerivatives 4.0 International (CC BY-ND 4.0)**
https://creativecommons.org/licenses/by-nd/4.0/
The translation is bundled **unmodified**, as the licence requires.
Credit: *Talal Itani, ClearQuran.com*

## 3. Urdu translation

**ترجمۂ قرآن by Maulana Fateh Muhammad Jalandhari (فتح محمد جالندھری)**
Status: **public domain**.
The text was **digitised by the JustQuran project** from a public-domain printed
scan (archive.org) and proofread against the scan — it is *not* copied from any
other digital source.
(Being rolled out as a free data update.)

## 4. Transliteration (romanisation)

**Tanzil Project — https://tanzil.net**
Quranic transliteration data © Tanzil Project. Used with attribution as required
by the Tanzil licence. Credit: *Tanzil Project (tanzil.net)*

## 5. Recitation audio

**Sheikh Mahmoud Khalil Al-Hussary (Murattal) — everyayah.com**
Per-verse MP3s (64 kbps) are **streamed** from `https://everyayah.com/data/Husary_64kbps`
at playback time; they are not bundled in this repository. When a user chooses
"download for offline", the files are cached on their own device only.

## 6. Fonts — SIL Open Font License 1.1

All fonts ship under the **SIL Open Font License, Version 1.1** (full text in
`public/fonts/OFL.txt`):

| Font file | Font | Copyright |
| --- | --- | --- |
| `digitalkhatt-indopak-v2.otf` | DigitalKhatt IndoPak | Amine Anane / Tarteel Inc |
| `amiri-quran.ttf` | Amiri Quran | The Amiri Project (Khaled Hosny) |
| `nastaliq.ttf` | Noto Nastaliq Urdu | Google Inc. |
| `noto-naskh.ttf` | Noto Naskh Arabic | Google Inc. |

The OFL permits bundling and redistribution; the fonts remain under the OFL and
must not be sold on their own. Reserved Font Names, where declared, belong to
their respective authors.

## 7. App icons & artwork

The JustQuran ring-and-wordmark icons (`public/icons/`) are original artwork
created for this project and are released under the same MIT License as the code.

---

## Deliberately NOT bundled

To keep the app legally clean, the following popular but restrictive sources are
**not** used anywhere in this project:

- quran.com / Quran Foundation API text (redistribution forbidden)
- Maududi, Saheeh International and Ahmed Ali translations (copyrighted)
- The Urdu file in `risan/quran-json` (it is Maududi's translation)
- KFGQPC (King Fahd Complex) fonts

If you contribute content, please make sure it is public domain, openly licensed,
or used with written permission — and add it to this file.
