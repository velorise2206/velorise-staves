@echo off
setlocal
set "ROOT=%~dp0.."
if not defined JAVA17_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe" (
        set "JAVA17_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
    )
)
if defined JAVA17_HOME (
    set "JAVA_HOME=%JAVA17_HOME%"
    set "PATH=%JAVA17_HOME%\bin;%PATH%"
)
echo Launching Forge 1.20.1 Client (JAVA_HOME=%JAVA_HOME%)...
cd /d "%ROOT%\1.20.1\forge"
call "%ROOT%\gradlew.bat" runClient --no-daemon
exit /b %ERRORLEVEL%
