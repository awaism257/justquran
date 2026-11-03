#!/bin/sh
# Builds the desktop bundle that the Flathub manifest downloads:
#   desktop/out/justquran-desktop-<version>.tar.gz
# Contents: the Electron shell (main.js, package.json), the built web app
# (www/), and the desktop entry, metainfo and icon.
set -eu

cd "$(dirname "$0")/.."
VERSION=$(sed -n 's/.*"version": "\(.*\)".*/\1/p' desktop/package.json)
NAME="justquran-desktop-$VERSION"
STAGE="desktop/out/$NAME"

npm run build

rm -rf "$STAGE" "desktop/out/$NAME.tar.gz"
mkdir -p "$STAGE"
cp desktop/main.js desktop/package.json desktop/app.justquran.JustQuran.desktop \
   desktop/app.justquran.JustQuran.metainfo.xml LICENSE "$STAGE/"
cp public/icons/icon-512.png "$STAGE/app.justquran.JustQuran.png"
cp -r dist "$STAGE/www"
# Website-only files that mean nothing in the desktop app.
rm -f "$STAGE/www/_headers" "$STAGE/www/_redirects" "$STAGE"/www/google*.html

tar --sort=name --mtime=@0 --owner=0 --group=0 --numeric-owner \
    -C desktop/out -czf "desktop/out/$NAME.tar.gz" "$NAME"
sha256sum "desktop/out/$NAME.tar.gz"
