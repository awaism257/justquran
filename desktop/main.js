// JustQuran desktop shell (Electron). Serves the bundled web app from the
// app:// scheme so reading works fully offline; audio streams from the same
// hosts as the website.

const { app, BrowserWindow, protocol, net, session, shell } = require('electron');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const ORIGIN = 'app://justquran';
const WWW = path.join(__dirname, 'www');

protocol.registerSchemesAsPrivileged([
  {
    scheme: 'app',
    privileges: { standard: true, secure: true, supportFetchAPI: true, corsEnabled: true, stream: true },
  },
]);

// Resolve a request path inside www/, falling back to index.html for
// client-side routes (as the website's _redirects does).
function resolveFile(pathname) {
  const rel = path.normalize(decodeURIComponent(pathname)).replace(/^([/\\])+/, '');
  const file = path.join(WWW, rel);
  if (file.startsWith(WWW + path.sep) && fs.statSync(file, { throwIfNoEntry: false })?.isFile()) {
    return file;
  }
  return path.join(WWW, 'index.html');
}

function isOwnUrl(url) {
  return url.startsWith(ORIGIN + '/');
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1100,
    height: 820,
    minWidth: 360,
    minHeight: 480,
    title: 'JustQuran',
    backgroundColor: '#101613',
    autoHideMenuBar: true,
    icon: path.join(WWW, 'icons', 'icon-512.png'),
    webPreferences: { contextIsolation: true, sandbox: true },
  });
  win.removeMenu();

  // Links to other sites (Ko-fi, books, privacy pages on the web) open in
  // the user's browser rather than inside the app.
  win.webContents.setWindowOpenHandler(({ url }) => {
    if (/^(https?|mailto):/.test(url)) shell.openExternal(url);
    return { action: 'deny' };
  });
  win.webContents.on('will-navigate', (event, url) => {
    if (isOwnUrl(url)) return;
    event.preventDefault();
    if (/^(https?|mailto):/.test(url)) shell.openExternal(url);
  });

  win.loadURL(ORIGIN + '/');
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    const [win] = BrowserWindow.getAllWindows();
    if (!win) return;
    if (win.isMinimized()) win.restore();
    win.focus();
  });

  app.whenReady().then(() => {
    protocol.handle('app', (request) => {
      const { pathname } = new URL(request.url);
      return net.fetch(pathToFileURL(resolveFile(pathname)).toString());
    });

    // The audio hosts only allow the website's origin, so let app:// read
    // their responses too (needed for downloading audio for offline use).
    session.defaultSession.webRequest.onHeadersReceived({ urls: ['https://*/*'] }, (details, callback) => {
      const headers = details.responseHeaders ?? {};
      if (details.webContentsId !== undefined) {
        for (const key of Object.keys(headers)) {
          if (key.toLowerCase().startsWith('access-control-allow-')) delete headers[key];
        }
        headers['Access-Control-Allow-Origin'] = [ORIGIN];
      }
      callback({ responseHeaders: headers });
    });

    createWindow();
  });

  app.on('window-all-closed', () => app.quit());
}
