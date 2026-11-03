# JustQuran desktop (Flatpak)

A small Electron shell around the web app, published on Flathub as
`app.justquran.JustQuran`. The web app is bundled, so reading works offline;
audio streams from the same hosts as the website.

## Releasing a new version

1. Bump `version` in `desktop/package.json` and add a `<release>` entry to
   `app.justquran.JustQuran.metainfo.xml`.
2. Run `desktop/build-bundle.sh`. It builds the web app and prints the
   SHA-256 of `desktop/out/justquran-desktop-<version>.tar.gz`.
3. Commit, tag `desktop-v<version>` and attach the tarball to a GitHub
   release with that tag (not marked as latest, so Android stays the latest).
4. In the Flathub repo (`flathub/app.justquran.JustQuran`), update the bundle
   URL and `sha256` in the manifest, and open a pull request.

`flathub/app.justquran.JustQuran.yml` is a copy of the Flathub manifest.
