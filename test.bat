@echo off
REM Compiles and runs the self-test suite (Windows)
cd /d "%~dp0"
if exist out-test rmdir /s /q out-test
mkdir out-test
dir /s /b src\*.java test\*.java > sources.txt
javac --release 11 -encoding UTF-8 -d out-test -cp "lib\*" @sources.txt || (del sources.txt & exit /b 1)
del sources.txt
xcopy /e /i /q resources out-test >nul
java -cp "out-test;lib\*" com.quiz.SelfTest
