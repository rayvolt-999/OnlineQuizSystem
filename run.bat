@echo off
REM Builds (if needed) and launches the Online Quiz System (Windows)
cd /d "%~dp0"
if not exist dist\OnlineQuizSystem.jar call build.bat
java -cp "dist\OnlineQuizSystem.jar;lib\*" com.quiz.Main
