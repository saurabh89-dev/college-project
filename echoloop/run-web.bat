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

"%JAVA_HOME%\bin\java.exe" -jar "target\echoloop-web-1.0.0.jar"

pause
endlocal