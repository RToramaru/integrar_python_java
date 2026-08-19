@echo off
setlocal

cd /d "%~dp0"
echo ========================================
echo  BUILD PYTHON + JAVA
echo ========================================

python build.py
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERRO] Build falhou com codigo %EXIT_CODE%.
    exit /b %EXIT_CODE%
)

exit /b 0
