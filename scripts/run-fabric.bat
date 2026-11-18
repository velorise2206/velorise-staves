@echo off
setlocal
set "ROOT=%~dp0.."
if not defined JAVA21_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe" (
        set "JAVA21_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
    )
)
if defined JAVA21_HOME (
    set "JAVA_HOME=%JAVA21_HOME%"
    set "PATH=%JAVA21_HOME%\bin;%PATH%"
)
echo Launching Fabric 1.21.1 Client (JAVA_HOME=%JAVA_HOME%)...
cd /d "%ROOT%\1.21.1\fabric"
call "%ROOT%\gradlew.bat" runClient --no-daemon
exit /b %ERRORLEVEL%
