@echo off
chcp 65001 >nul 2>&1
title Library Backend Server (port 8080)

echo ============================================
echo   Library Location System - Backend
echo   Port: 8080
echo   Close this window to stop the server
echo ============================================
echo.

cd /d "%~dp0backend"
if errorlevel 1 (
    echo [ERROR] Cannot enter backend directory
    pause
    exit /b 1
)

netstat -ano | findstr ":8080.*LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [WARN] Port 8080 is in use, releasing...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
        echo   Killing PID %%a
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo   Port released.
    echo.
)

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found, please install JDK and set JAVA_HOME.
    pause
    exit /b 1
)

echo Starting backend server, please wait...
echo.

call .\mvnw.cmd spring-boot:run

echo.
echo Server stopped.
pause
