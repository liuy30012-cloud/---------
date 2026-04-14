@echo off
chcp 65001 >nul 2>&1
echo Stopping backend server on port 8080...

:: 找到占用 8080 端口的进程并终止
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080.*LISTENING"') do (
    echo Killing PID %%a
    taskkill /PID %%a /F 2>nul
)

echo Done.
pause
