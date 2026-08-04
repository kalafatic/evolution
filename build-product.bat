@echo off
setlocal enabledelayedexpansion

echo ============================================================
echo Starting Headless RCP Build of EVO via build-product.bat...
echo ============================================================

REM Find script and repository root
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM Determine target platform
set "PLATFORM=windows"
if "%~1"=="--platform" (
    set "PLATFORM=%~2"
) else if "%~1"=="-Platform" (
    set "PLATFORM=%~2"
)

REM We can run build-product.ps1 which is extremely robust and cross-platform
if exist "build-product.ps1" (
    echo Delegating build to build-product.ps1 with platform: !PLATFORM!
    powershell -NoProfile -ExecutionPolicy Bypass -File "build-product.ps1" -Platform !PLATFORM!
    exit /b !ERRORLEVEL!
)

REM Fallback directly to maven wrapper if ps1 not found
if not exist "mvnw.cmd" (
    echo ERROR: maven wrapper 'mvnw.cmd' not found in repository root!
    exit /b 1
)

set "PROFILE=-Pwindows"
if "!PLATFORM!"=="linux" set "PROFILE=-Plinux"
if "!PLATFORM!"=="all" set "PROFILE=-Pall-platforms"

echo Running maven clean verify on repository...
call mvnw.cmd clean verify -DskipTests !PROFILE!
set BUILD_EXIT_CODE=%ERRORLEVEL%

if %BUILD_EXIT_CODE% neq 0 (
    echo ERROR: Headless RCP Build FAILED with exit code %BUILD_EXIT_CODE%
    exit /b %BUILD_EXIT_CODE%
)

REM Create release directory
if not exist "release" mkdir "release"

REM Locate and copy final artifact(s)
set "PROD_FOUND=0"
if exist "eu.kalafatic.evolution.repository\target\products\evolution-win32.win32.x86_64.zip" (
    copy /y "eu.kalafatic.evolution.repository\target\products\evolution-win32.win32.x86_64.zip" "release\EVO-win-x64.zip"
    echo SUCCESS: Published Windows x64 package: release\EVO-win-x64.zip
    set "PROD_FOUND=1"
)
if exist "eu.kalafatic.evolution.repository\target\products\evolution-linux.gtk.x86_64.tar.gz" (
    copy /y "eu.kalafatic.evolution.repository\target\products\evolution-linux.gtk.x86_64.tar.gz" "release\EVO-linux-x64.tar.gz"
    echo SUCCESS: Published Linux x64 package: release\EVO-linux-x64.tar.gz
    set "PROD_FOUND=1"
)

if "!PROD_FOUND!"=="0" (
    echo ERROR: Product was built but final packaged archive files could not be located.
    exit /b 1
)

echo ============================================================
echo Headless RCP Build and Packaging COMPLETED successfully.
echo ============================================================
exit /b 0
