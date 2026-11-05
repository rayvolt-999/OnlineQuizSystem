@echo off
REM Compiles the project into out\ and packages dist\OnlineQuizSystem.jar (Windows)
cd /d "%~dp0"
where javac >nul 2>nul || (echo javac not found. Install JDK 11 or newer and add it to PATH. & exit /b 1)

if exist out rmdir /s /q out
if exist dist rmdir /s /q dist
mkdir out
mkdir dist

dir /s /b src\*.java > sources.txt
javac --release 11 -encoding UTF-8 -d out -cp "lib\*" @sources.txt || (del sources.txt & exit /b 1)
del sources.txt
xcopy /e /i /q resources out >nul

for %%f in (lib\sqlite-jdbc-*.jar) do set JARNAME=%%~nxf
(echo Main-Class: com.quiz.Main& echo Class-Path: lib/%JARNAME%) > out\MANIFEST.MF
jar cfm dist\OnlineQuizSystem.jar out\MANIFEST.MF -C out com -C out seed
mkdir dist\lib
copy lib\*.jar dist\lib >nul
if exist config.properties copy config.properties dist >nul
echo Done. Run with run.bat
