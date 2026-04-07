const { app, BrowserWindow } = require('electron');
const path = require('path');
const isDev = process.env.NODE_ENV === 'development';

function createWindow() {
  const mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 1024,
    minHeight: 768,
    titleBarStyle: 'hidden',
    titleBarOverlay: {
      color: '#f8f9fb',
      symbolColor: '#2b3438',
      height: 48
    },
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true
    }
  });

  if (isDev) {
    // In dev mode, load the vite dev server
    mainWindow.loadURL('http://localhost:5173');
    // mainWindow.webContents.openDevTools();
  } else {
    // In production, load the built index.html
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', function () {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', function () {
  if (process.platform !== 'darwin') app.quit();
});
