#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ULTIMATE_ROOT="$SCRIPT_DIR/../../../.."
BAZELRC_USER="$ULTIMATE_ROOT/.bazelrc-user.bazelrc"

docker rm -f bazel-rbe-local 2>/dev/null && echo "NativeLink stopped." || echo "Not running."

if [ -f "$BAZELRC_USER" ] && grep -q "start-local-rbe.sh" "$BAZELRC_USER" 2>/dev/null; then
  rm "$BAZELRC_USER"
  echo "Removed $BAZELRC_USER"
fi
