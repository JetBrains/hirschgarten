#!/bin/bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

ensure_prerequisites
ensure_state_directories
ensure_bazel_source_checkout

"$RBE_SCRIPT_DIR/start.sh" "$TARGET_WORKSPACE_ROOT"
write_user_bazelrcs

echo
echo "Activated local remote execution for:"
echo "  $TARGET_WORKSPACE_ROOT"
if [ -n "$PLUGIN_USER_BAZELRC" ]; then
  echo "  $TARGET_PLUGIN_ROOT"
fi
echo
print_status_summary
print_bazelrc_hook_notes
echo
echo "Next steps:"
echo "  1. Open the target workspace in the IDE as usual."
echo "  2. Run Bazel commands normally. The generated user-local bazelrc files now enable --config=rbe-local automatically and route both remote execution and remote cache to the local worker for that profile."
echo "  3. Use $RBE_SCRIPT_DIR/stop.sh to stop the worker."
