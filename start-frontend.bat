@echo off
setlocal
chcp 65001 >nul 2>&1
title Library Frontend Dev Server (Vite)

echo ============================================
echo   Library Locator - Frontend Dev Server
echo   Port: 5173
echo   Close this window to stop the server
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "FRONTEND_DIR=%SCRIPT_DIR%frontend"

if not exist "%FRONTEND_DIR%\" (
    echo [ERROR] Frontend directory not found:
    echo %FRONTEND_DIR%
    echo.
    pause
    exit /b 1
)

cd /d "%FRONTEND_DIR%"

where npm >nul 2>&1
if errorlevel 1 (
    echo [ERROR] npm was not found in PATH.
    echo Install Node.js and try again.
    echo.
    pause
    exit /b 1
)

if not exist "package.json" (
    echo [ERROR] package.json was not found in:
    echo %CD%
    echo.
    pause
    exit /b 1
)

if not exist "node_modules\.bin\vite.cmd" (
    echo [INFO] Frontend dependencies are missing.
    echo [INFO] Running npm ci...
    call npm ci
    if errorlevel 1 (
        echo.
        echo [ERROR] npm ci failed. Please fix the npm error above and retry.
        pause
        exit /b 1
    )
    echo.
)

call npm run dev
set "EXIT_CODE=%ERRORLEVEL%"

echo.
if not "%EXIT_CODE%"=="0" (
    echo [ERROR] Dev server exited with code %EXIT_CODE%.
) else (
    echo Dev server stopped.
)
pause
exit /b %EXIT_CODE%
