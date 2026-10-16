@echo off
setlocal
title EchoLoop Web Launcher

cd /d "%~dp0"

set "JAVA_HOME=C:\Program Files\Java\jdk-26.0.2"
set "MVN=C:\Users\kumar\Downloads\apache-maven-3.10.0-bin\apache-maven-3.10.0\bin\mvn.cmd"
set "MAVEN_OPTS=-Dmaven.repo.local=%CD%\.m2"

if not exist "%JAVA_HOME%\bin\java.exe" (
    echo ERROR: Java was not found.
    pause
    exit /b 1
)

if not exist "%MVN%" (
    echo ERROR: Maven was not found.
    pause
    exit /b 1
)

echo.
echo Ensure MySQL is running in XAMPP first.
echo.
echo [1/2] Building EchoLoop Web...
call "%MVN%" clean package -DskipTests

if errorlevel 1 (
    echo.
    echo ERROR: Build failed.
    pause
    exit /b 1
)

echo.
echo [2/2] Starting at http://localhost:8080
echo Keep this window open while using EchoLoop.
echo Press Ctrl+C to stop.
echo.

start "EchoLoop Server" /b "%JAVA_HOME%\bin\java.exe" -jar "target\echoloop-web-1.0.0.jar"

echo Waiting for EchoLoop to start...
set /a ATTEMPTS=0

:WAIT_FOR_ECHOLOOP
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -Command "try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 1 http://localhost:8080 | Out-Null; exit 0 } catch { exit 1 }" >nul 2>&1
if not errorlevel 1 goto OPEN_BROWSER

set /a ATTEMPTS+=1
if %ATTEMPTS% GEQ 30 goto OPEN_BROWSER
timeout /t 1 /nobreak >nul
goto WAIT_FOR_ECHOLOOP

:OPEN_BROWSER
set "CHROME=%ProgramFiles%\Google\Chrome\Application\chrome.exe"
if not exist "%CHROME%" set "CHROME=%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe"

echo.
echo Opening EchoLoop at http://localhost:8080
if exist "%CHROME%" (
    start "EchoLoop" "%CHROME%" "http://localhost:8080"
) else (
    echo Google Chrome was not found. Opening your default browser instead.
    start "EchoLoop" "http://localhost:8080"
)

pause
endlocal
