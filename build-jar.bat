@echo off
echo Building Spring Boot JAR file...
echo.

cd /d "%~dp0"
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Build successful!
    echo ========================================
    echo.
    echo JAR file location:
    dir target\restrosuite-*.jar /b
    echo.
) else (
    echo.
    echo ========================================
    echo Build failed!
    echo ========================================
    echo.
)

pause

