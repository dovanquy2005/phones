@echo off
title Staff API - Phone Store
color 0A

echo =======================================================
echo   🚀 Starting Staff API (Spring Boot)
echo =======================================================
echo.

REM Di chuyển tới thư mục chứa file JAR
cd /d "%~dp0"

REM Kiểm tra đã build JAR chưa
if not exist "target\staff-api-0.0.1-SNAPSHOT.jar" (
    echo [INFO] JAR not found, building project...
    call .\mvnw clean package -DskipTests
    echo.
)

echo [INFO] Launching application...
echo.

REM Chạy ứng dụng trên cổng 9090
java -jar target\staff-api-0.0.1-SNAPSHOT.jar --server.port=9090

echo.
echo =======================================================
echo   ✅ Staff API stopped. Press any key to exit.
echo =======================================================
pause >nul
