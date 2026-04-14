@echo off
chcp 65001 >nul 2>&1
title Library Frontend Dev Server (Vite)

echo ============================================
echo   图书馆定位系统 - 前端开发服务器启动
echo   Port: 5173
echo   Close this window to stop the server
echo ============================================
echo.

cd /d "%~dp0frontend"

npm run dev

echo.
echo Dev server stopped.
pause
