@echo off
setlocal
cd /d "%~dp0..\run"

if not exist paper.jar (
    echo Paper fehlt: %CD%\paper.jar
    echo Fuehre zuerst die Server-Einrichtung aus.
    pause
    exit /b 1
)

java -Xms2G -Xmx2G -jar paper.jar --nogui
pause
