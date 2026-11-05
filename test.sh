#!/usr/bin/env bash
# Compiles and runs the self-test suite (uses a temporary database, never touches data/quiz.db).
set -e
cd "$(dirname "$0")"
rm -rf out-test
mkdir -p out-test
javac --release 11 -encoding UTF-8 -d out-test -cp "lib/*" $(find src test -name "*.java")
cp -r resources/* out-test/
java -cp "out-test:lib/*" com.quiz.SelfTest
