@echo off
setlocal
echo [Velorise's Staves Fabric 1.21.1] Building mod jar...
call gradlew.bat build --no-daemon

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Mod built successfully! Output located in build\libs\
pause

