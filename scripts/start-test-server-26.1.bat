@echo off
setlocal
if defined AOC_TEST_SERVER_DIR (
    cd /d "%AOC_TEST_SERVER_DIR%"
) else (
    cd /d "%~dp0..\run-26.1"
)

if not exist paper.jar (
    echo Paper 26.1.2 fehlt: %CD%\paper.jar
    echo Fuehre zuerst die 26.1-Testserver-Einrichtung aus.
    pause
    exit /b 1
)

java -Xms2G -Xmx2G -jar paper.jar --nogui
if not defined CI pause
