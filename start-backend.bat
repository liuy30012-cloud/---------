@echo off
chcp 65001 >nul 2>&1
title Library Backend Server (port 8080)

echo ============================================
echo   图书馆定位系统 - 后端服务启动
echo   Port: 8080
echo   Close this window to stop the server
echo ============================================
echo.

cd /d "%~dp0backend"

mvnw.cmd spring-boot:run

echo.
echo Server stopped.
pause
