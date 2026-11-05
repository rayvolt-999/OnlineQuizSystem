#!/usr/bin/env bash
# Compiles the project into out/ and packages dist/OnlineQuizSystem.jar
set -e
cd "$(dirname "$0")"

if ! command -v javac >/dev/null 2>&1; then
  echo "javac not found. Install a JDK 11 or newer (Fedora: sudo dnf install java-21-openjdk-devel)."
  exit 1
fi

CP="lib/*"
rm -rf out dist
mkdir -p out dist

echo "Compiling sources..."
javac --release 11 -encoding UTF-8 -d out -cp "$CP" $(find src -name "*.java")
cp -r resources/* out/

echo "Packaging dist/OnlineQuizSystem.jar..."
JARNAME=$(basename lib/sqlite-jdbc-*.jar)
printf "Main-Class: com.quiz.Main\nClass-Path: lib/%s\n" "$JARNAME" > out/MANIFEST.MF
jar cfm dist/OnlineQuizSystem.jar out/MANIFEST.MF -C out com -C out seed
mkdir -p dist/lib
cp lib/*.jar dist/lib/
cp config.properties dist/ 2>/dev/null || true

echo "Done. Run with ./run.sh"
