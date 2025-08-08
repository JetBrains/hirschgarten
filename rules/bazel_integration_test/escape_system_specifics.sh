#!/usr/bin/env bash
set -euo pipefail

# Build a sed script that applies every replacement

has_flag() {
  local needle="$1"; shift
  for arg in "$@"; do
    case "$arg" in
      --) break ;;
      "$needle") return 0 ;;
    esac
  done
  return 1
}

make_sed_script() {
  if ! has_flag "--no-aspects-output" "$@"; then
  cat <<'SED'
s|"bazel-out/[^"]*fastbuild/bin"|"bazel-out/fastbuild/bin"|g
s|"bazel-out/[^"]*opt-exec[^"]*/bin"|"bazel-out/opt-exec/bin"|g
s|"external/python[^"]*"|"external/python"|g
s|"external/remotejdk[^"]*"|"external/remotejdk"|g
s|"external/rules_java++toolchains+remotejdk[^"]*"|"external/rules_java++toolchains+remotejdk"|g
s|"external/rules_java~[^"]*~toolchains~remotejdk[^"]*"|"external/rules_java~~toolchains~remotejdk"|g
s|"external/rules_python++python+python[^"]*"|"external/rules_python++python+python"|g
s|"external/rules_python~[^"]*~python~python[^"]*"|"external/rules_python~~python~python"|g
SED
  fi
  if has_flag "--check-diagnostics" "$@"; then
    cat <<'SED'
s|textDocument=TextDocumentIdentifier(path=[^\)]*/\([^)]*\))|textDocument=TextDocumentIdentifier(path=\1)|g
s/\x1b\[[0-9;?]*[ -/]*[@-~]//g
SED
  fi
}

make_sed_script "$@" | sed -f /dev/stdin -- "$1"