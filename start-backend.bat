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

:: 检查端口 8080 是否已被占用
netstat -ano | findstr ":8080.*LISTENING" >nul 2>&1
if %errorlevel%==0 (
    echo [警告] 端口 8080 已被占用，正在尝试释放...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
        echo   终止进程 PID %%a
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo   端口已释放。
    echo.
)

:: 检查 Java 是否可用
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 Java，请确保已安装 JDK 并配置了 JAVA_HOME。
    pause
    exit /b 1
)

echo 正在启动后端服务，请耐心等待...
echo.

call mvnw.cmd spring-boot:run

echo.
echo Server stopped.
pause
