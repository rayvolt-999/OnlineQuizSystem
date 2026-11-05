#!/usr/bin/env bash
# Builds (if needed) and launches the Online Quiz System.
set -e
cd "$(dirname "$0")"
if [ ! -f dist/OnlineQuizSystem.jar ] || [ -n "$(find src resources -newer dist/OnlineQuizSystem.jar -type f 2>/dev/null | head -1)" ]; then
  ./build.sh
fi
java -cp "dist/OnlineQuizSystem.jar:lib/*" com.quiz.Main
