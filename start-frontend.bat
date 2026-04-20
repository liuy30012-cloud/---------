@echo off
chcp 65001 >nul 2>&1
title Library Frontend Dev Server (Vite)

echo ============================================
echo   Library Location System - Frontend
echo   Port: 5173
echo   Close this window to stop the server
echo ============================================
echo.

cd /d "%~dp0frontend"
if errorlevel 1 (
    echo [ERROR] Cannot enter frontend directory
    pause
    exit /b 1
)

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm not found, please install Node.js
    pause
    exit /b 1
)

call npm run dev

echo.
echo Dev server stopped.
pause
