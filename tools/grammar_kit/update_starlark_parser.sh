#!/bin/bash
# Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
# Regenerates _StarlarkParser.java and StarlarkTokenTypes.java from Starlark.bnf with Grammar-Kit, without the IDE.
# Needs JDK 25+ and one prior Bazel build, for the platform jars and languages.jar:
#   ./tests.cmd --module intellij.bazel.pluginTests --test org.jetbrains.bazel.languages.starlark.parser.StarlarkParsingStatementTest
# Usage: update_starlark_parser.sh, or GRAMMAR_KIT_JAR=... update_starlark_parser.sh to use a local jar.

set -euo pipefail

GRAMMAR_KIT_VERSION="2023.3.4"
GRAMMAR_KIT_URL="https://cache-redirector.jetbrains.com/maven-central/org/jetbrains/grammar-kit/$GRAMMAR_KIT_VERSION/grammar-kit-$GRAMMAR_KIT_VERSION.jar"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../../../.." && pwd)"
CACHE_DIR="${TMPDIR:-/tmp}/intellij-starlark-grammar"
FD="$REPO_DIR/community/tools/fd.cmd"

GEN_ROOT="$REPO_DIR/plugins/bazel/intellij.bazel.languages/src"
PARSER_DIR="$GEN_ROOT/org/jetbrains/bazel/languages/starlark/parser"
BNF_FILE="$PARSER_DIR/Starlark.bnf"
GENERATED_FILES=("$PARSER_DIR/_StarlarkParser.java" "$GEN_ROOT/org/jetbrains/bazel/languages/starlark/elements/StarlarkTokenTypes.java")

resolve_java() {
  local min="$1"
  if [ -n "${JAVA_HOME:-}" ]; then
    echo "$JAVA_HOME/bin/java"; return
  fi
  if command -v /usr/libexec/java_home &>/dev/null; then
    local home
    home=$(/usr/libexec/java_home -v "$min" 2>/dev/null || true)
    if [ -n "$home" ]; then echo "$home/bin/java"; return; fi
  fi
  command -v java || true
}

java_major() {
  "$1" -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/'
}

JAVA_BIN="$(resolve_java 25)"
if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ]; then
  echo "Error: no JDK found. Set JAVA_HOME." >&2
  exit 1
fi

resolve_grammar_kit_jar() {
  if [ -n "${GRAMMAR_KIT_JAR:-}" ]; then
    [ -f "$GRAMMAR_KIT_JAR" ] || { echo "Error: GRAMMAR_KIT_JAR does not exist: $GRAMMAR_KIT_JAR" >&2; exit 1; }
    return
  fi
  GRAMMAR_KIT_JAR="$CACHE_DIR/grammar-kit-$GRAMMAR_KIT_VERSION.jar"
  if [ ! -f "$GRAMMAR_KIT_JAR" ]; then
    echo "Downloading Grammar-Kit $GRAMMAR_KIT_VERSION..."
    mkdir -p "$CACHE_DIR"
    curl -sSL --fail -o "$GRAMMAR_KIT_JAR.tmp" "$GRAMMAR_KIT_URL"
    mv "$GRAMMAR_KIT_JAR.tmp" "$GRAMMAR_KIT_JAR"
  fi
}

find_platform_runfiles() {
  local bazel_bin
  bazel_bin="$REPO_DIR/out/bazel-bin"
  [ -e "$bazel_bin" ] || return 1
  bazel_bin="$(cd "$bazel_bin" && pwd)"
  local marker
  marker=$("$FD" -H -t d --max-depth 4 '\.runfiles$' "$bazel_bin" 2>/dev/null | while read -r d; do
    if [ -f "$d/community+/platform/util/util.jar" ] && [ -f "$d/_main/plugins/bazel/intellij.bazel.languages/languages.jar" ]; then
      echo "$d"; break
    fi
  done)
  [ -n "$marker" ] || return 1
  echo "$marker"
}

generate_parser() {
  [ -f "$BNF_FILE" ] || { echo "Error: not found: $BNF_FILE" >&2; exit 1; }

  local major
  major="$(java_major "$JAVA_BIN")"
  if [ "${major:-0}" -lt 25 ]; then
    echo "Error: parser generation needs JDK 25+ (platform classes are class file 69); found $major." >&2
    echo "       Unset JAVA_HOME or point it at a JBR 25." >&2
    exit 1
  fi

  local runfiles
  if ! runfiles="$(find_platform_runfiles)"; then
    echo "Error: no Bazel runfiles tree with IntelliJ platform jars and languages.jar found." >&2
    echo "       Populate it with one build, then re-run:" >&2
    echo "         ./tests.cmd --module intellij.bazel.pluginTests --test org.jetbrains.bazel.languages.starlark.parser.StarlarkParsingStatementTest" >&2
    exit 1
  fi

  resolve_grammar_kit_jar
  echo "Using Grammar-Kit $GRAMMAR_KIT_VERSION: $GRAMMAR_KIT_JAR"

  local work jars
  work="$CACHE_DIR/cp"
  mkdir -p "$work"
  jars="$work/jars.txt"

  # Some fat jars bundle a stale copy of the platform, so they are excluded and the platform jars go first.
  "$FD" -H -e jar . "$runfiles" 2>/dev/null \
    | grep -v '_test_lib\.jar$' \
    | grep -vE 'analysis-kotlin|dokka|kotlin-compiler|-all\.jar$' > "$jars.raw"
  grep '/platform/' "$jars.raw" > "$jars" || true
  grep -v '/platform/' "$jars.raw" >> "$jars" || true

  local argfile ide_home
  argfile="$work/argfile.txt"
  ide_home="$work/ide-home"
  mkdir -p "$ide_home/config" "$ide_home/system" "$ide_home/plugins"
  {
    printf -- '-cp\n'
    printf '%s\n' "$GRAMMAR_KIT_JAR:$(tr '\n' ':' < "$jars")"
  } > "$argfile"

  rm -f "${GENERATED_FILES[@]}"

  local log
  log="$work/generate.log"
  echo "Generating parser from ${BNF_FILE#"$REPO_DIR/"}..."
  "$JAVA_BIN" \
    -Didea.home.path="$ide_home" \
    -Didea.config.path="$ide_home/config" \
    -Didea.system.path="$ide_home/system" \
    -Didea.plugins.path="$ide_home/plugins" \
    "@$argfile" org.intellij.grammar.Main "$GEN_ROOT" "$BNF_FILE" \
    2>&1 | grep -vE '^WARNING|^WARN:|TelemetryManager|^\s+at |^java\.lang\.Throwable' | tee "$log" || true

  for f in "${GENERATED_FILES[@]}"; do
    [ -f "$f" ] || { echo "Error: Grammar-Kit did not produce $f" >&2; exit 1; }
    if grep -qE '/(Users|home)/' "$f"; then
      echo "Error: machine-specific absolute path leaked into $f" >&2; exit 1
    fi
  done
  if grep -qiE 'class not found|method not found' "$log"; then
    echo "Error: Grammar-Kit did not see StarlarkParserUtil. Build the module, then re-run." >&2; exit 1
  fi
  echo "  parser: $(wc -l < "${GENERATED_FILES[0]}" | tr -d ' ') lines"
}

generate_parser
