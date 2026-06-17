@echo off
setlocal enabledelayedexpansion
title NeuroCast - Construire pachet portabil

REM ================================================================
REM  Script de construire a aplicatiei NeuroCast (pachet portabil)
REM  Dublu-click pe acest fisier pentru a genera NeuroCast-windows.zip
REM ================================================================

REM Mergem in folderul scriptului (radacina proiectului)
cd /d "%~dp0"

set "JAR_NAME=SistemPreviziuneVanzari-1.0-SNAPSHOT.jar"
set "MAIN_CLASS=ro.licenta.analiza.Launcher"
set "MVN=%~dp0_buildtools\maven\bin\mvn.cmd"

echo.
echo ==========================================================
echo   NeuroCast - construire pachet portabil
echo ==========================================================
echo.

REM --- Verificare Maven local ---
if not exist "%MVN%" (
  echo [EROARE] Nu am gasit Maven la: %MVN%
  echo Asigura-te ca folderul _buildtools\maven exista.
  pause
  exit /b 1
)

REM --- Detectare JAVA_HOME din jpackage (daca nu e setat) ---
if not defined JAVA_HOME (
  for /f "delims=" %%i in ('where jpackage 2^>nul') do set "JP_PATH=%%i"
  if defined JP_PATH (
    for %%i in ("!JP_PATH!") do set "JP_BIN=%%~dpi"
    for %%i in ("!JP_BIN!\..") do set "JAVA_HOME=%%~fi"
  )
)
echo JAVA_HOME = %JAVA_HOME%
echo.

REM --- PAS 1: Compilare si impachetare jar ---
echo [1/6] Compilez codul si creez jar-ul...
call "%MVN%" -q clean package -DskipTests
if errorlevel 1 ( echo [EROARE] Compilarea a esuat. & pause & exit /b 1 )

REM --- PAS 2: Adunare dependinte ---
echo [2/6] Adun bibliotecile (POI, iText, JavaFX)...
call "%MVN%" -q dependency:copy-dependencies -DoutputDirectory=target/lib -DincludeScope=runtime
if errorlevel 1 ( echo [EROARE] Adunarea dependintelor a esuat. & pause & exit /b 1 )

REM --- PAS 3: Pregatire folder de intrare pentru jpackage ---
echo [3/6] Pregatesc fisierele pentru impachetare...
if exist "target\jpackage-input" rmdir /s /q "target\jpackage-input"
mkdir "target\jpackage-input\lib"
copy /y "target\%JAR_NAME%" "target\jpackage-input\" >nul
copy /y "target\lib\*.jar" "target\jpackage-input\lib\" >nul

REM --- PAS 4: Construire aplicatie cu Java inclus ---
echo [4/6] Construiesc aplicatia cu jpackage (poate dura 1-2 minute)...
if exist "dist\NeuroCast" rmdir /s /q "dist\NeuroCast"
if not exist "dist" mkdir "dist"
jpackage --type app-image --name NeuroCast --input "target\jpackage-input" --main-jar "%JAR_NAME%" --main-class "%MAIN_CLASS%" --dest "dist" --vendor "Licenta"
if errorlevel 1 ( echo [EROARE] jpackage a esuat. & pause & exit /b 1 )

REM --- PAS 5: Adaugare fisiere extra langa exe ---
echo [5/6] Adaug fisierul de date exemplu si instructiunile...
if exist "date_fictive_vanzari_v1.xlsx" copy /y "date_fictive_vanzari_v1.xlsx" "dist\NeuroCast\" >nul
if exist "CITESTE-MA.txt" copy /y "CITESTE-MA.txt" "dist\NeuroCast\" >nul

REM --- PAS 6: Comprimare in zip ---
echo [6/6] Creez arhiva NeuroCast-windows.zip...
if exist "NeuroCast-windows.zip" del /f /q "NeuroCast-windows.zip"
powershell -NoProfile -Command "Compress-Archive -Path 'dist\NeuroCast' -DestinationPath 'NeuroCast-windows.zip' -CompressionLevel Optimal -Force"
if errorlevel 1 ( echo [EROARE] Crearea arhivei a esuat. & pause & exit /b 1 )

echo.
echo ==========================================================
echo   GATA! Arhiva a fost creata:
echo   %~dp0NeuroCast-windows.zip
echo ==========================================================
echo.
echo Urca acest fisier .zip ca Release pe GitHub.
echo.
pause
