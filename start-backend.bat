@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1

cd /d "%~dp0backend"
if errorlevel 1 (
    echo [ERROR] Cannot enter backend directory
    pause
    exit /b 1
)

if exist ".env" (
    call :load_env_file ".env"
)

if "%SERVER_PORT%"=="" set "SERVER_PORT=8080"

title Library Backend Server (port %SERVER_PORT%)

echo ============================================
echo   Library Location System - Backend
echo   Port: %SERVER_PORT%
echo   Close this window to stop the server
echo ============================================
echo.

if not exist ".\mvnw.cmd" (
    echo [ERROR] Maven Wrapper not found: backend\mvnw.cmd
    pause
    exit /b 1
)

java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found, please install JDK and set JAVA_HOME.
    pause
    exit /b 1
)

netstat -ano | findstr ":%SERVER_PORT%.*LISTENING" >nul 2>&1
if not errorlevel 1 (
    echo [WARN] Port %SERVER_PORT% is in use, releasing...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%SERVER_PORT%.*LISTENING"') do (
        echo   Killing PID %%a
        taskkill /PID %%a /F >nul 2>&1
    )
    timeout /t 2 /nobreak >nul
    echo   Port released.
    echo.
)

echo Starting backend server, please wait...
echo.

if "%JWT_SECRET%"=="" set "JWT_SECRET=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
if "%ANTI_CRAWLER_SECRET%"=="" set "ANTI_CRAWLER_SECRET=dev-anti-crawler-secret-0123456789abcdef"
if "%WATERMARK_SECRET%"=="" set "WATERMARK_SECRET=dev-watermark-secret-0123456789abcdef"
if "%DB_URL%"=="" (
    if /I "%BACKEND_DB_MODE%"=="file-h2" (
        set "DB_URL=jdbc:h2:file:./data/library-dev;MODE=MySQL;NON_KEYWORDS=YEAR;AUTO_SERVER=TRUE"
        echo [INFO] No DB_URL configured, using persistent H2 file database.
    ) else (
        set "DB_URL=jdbc:h2:mem:library-dev;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        echo [INFO] No DB_URL configured, using in-memory H2 database for quick start.
        echo [INFO] Set DB_URL or BACKEND_DB_MODE=file-h2 if you want persistent local data.
    )
) else (
    echo [INFO] Using DB_URL from environment or backend\.env.
)

if "%DB_DRIVER%"=="" call :infer_db_driver
call :print_db_summary

call .\mvnw.cmd -Dmaven.test.skip=true spring-boot:run
set "APP_EXIT_CODE=%errorlevel%"

echo.
echo Server stopped.
pause
exit /b %APP_EXIT_CODE%

:load_env_file
for /f "usebackq tokens=1,* delims== eol=#" %%A in ("%~1") do (
    if not defined %%A set "%%A=%%B"
)
exit /b 0

:infer_db_driver
set "_db_url=%DB_URL%"
if not "%_db_url:jdbc:mysql=%"=="%_db_url%" (
    set "DB_DRIVER=com.mysql.cj.jdbc.Driver"
    set "_db_url="
    exit /b 0
)
if not "%_db_url:jdbc:h2=%"=="%_db_url%" (
    set "DB_DRIVER=org.h2.Driver"
    set "_db_url="
    exit /b 0
)
set "_db_url="
exit /b 0

:print_db_summary
set "_db_url=%DB_URL%"
if not "%_db_url:jdbc:mysql=%"=="%_db_url%" (
    echo [INFO] Active database: MySQL
    echo [INFO] Active JDBC driver: %DB_DRIVER%
    set "_db_url="
    exit /b 0
)
if not "%_db_url:jdbc:h2:file=%"=="%_db_url%" (
    echo [INFO] Active database: H2 file
    echo [INFO] Active JDBC driver: %DB_DRIVER%
    set "_db_url="
    exit /b 0
)
if not "%_db_url:jdbc:h2:mem=%"=="%_db_url%" (
    echo [INFO] Active database: H2 in-memory
    echo [INFO] Active JDBC driver: %DB_DRIVER%
    set "_db_url="
    exit /b 0
)
echo [INFO] Active JDBC driver: %DB_DRIVER%
set "_db_url="
exit /b 0
