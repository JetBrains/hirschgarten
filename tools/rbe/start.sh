#!/bin/bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

ensure_prerequisites
ensure_state_directories
ensure_bazel_source_checkout
cleanup_stale_pid_files
build_worker_binary

if worker_is_running; then
  write_runtime_config
  echo "Local remote worker is already running on grpc://127.0.0.1:$RBE_PORT (pid $(worker_pid))."
  exit 0
fi

ensure_port_available
rm -f "$RBE_PID_FILE" "$RBE_LAUNCHER_PID_FILE"

worker_binary="$(worker_binary_path)"
[ -x "$worker_binary" ] || {
  echo "Could not find the built remote worker at $worker_binary" >&2
  exit 1
}

worker_args=(
  --listen_port="$RBE_PORT"
  --work_path="$RBE_WORK_PATH"
  --cas_path="$RBE_CAS_PATH"
  --pid_file="$RBE_PID_FILE"
)

linux_sandbox="$(linux_sandbox_args)"
if [ -n "$linux_sandbox" ]; then
  # shellcheck disable=SC2206
  worker_args+=($linux_sandbox)
fi

(
  cd "$BAZEL_SOURCE_DIR"
  nohup "$worker_binary" "${worker_args[@]}" >>"$RBE_LOG_PATH" 2>&1 </dev/null &
  echo "$!" > "$RBE_LAUNCHER_PID_FILE"
)

wait_for_worker
write_runtime_config

echo "Started local remote worker on grpc://127.0.0.1:$RBE_PORT."
echo "Log file: $RBE_LOG_PATH"
