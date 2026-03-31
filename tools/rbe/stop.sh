#!/bin/bash

set -euo pipefail

source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

cleanup_stale_pid_files

effective_worker_pid_value="$(effective_worker_pid || true)"

if [ -z "$effective_worker_pid_value" ] && [ -z "$(launcher_pid || true)" ]; then
  echo "Local remote worker is not running."
  exit 0
fi

worker_pid_value="$effective_worker_pid_value"
launcher_pid_value="$(launcher_pid || true)"

if [ -n "$worker_pid_value" ] && is_pid_running "$worker_pid_value"; then
  kill "$worker_pid_value" >/dev/null 2>&1 || true
fi

if [ -n "$launcher_pid_value" ] && [ "$launcher_pid_value" != "$worker_pid_value" ] && is_pid_running "$launcher_pid_value"; then
  kill "$launcher_pid_value" >/dev/null 2>&1 || true
fi

for _ in $(seq 1 20); do
  if [ -n "$worker_pid_value" ] && is_pid_running "$worker_pid_value"; then
    sleep 1
    continue
  fi
  break
done

if [ -n "$worker_pid_value" ] && is_pid_running "$worker_pid_value"; then
  kill -9 "$worker_pid_value" >/dev/null 2>&1 || true
fi

if [ -n "$launcher_pid_value" ] && [ "$launcher_pid_value" != "$worker_pid_value" ] && is_pid_running "$launcher_pid_value"; then
  kill -9 "$launcher_pid_value" >/dev/null 2>&1 || true
fi

rm -f "$RBE_PID_FILE" "$RBE_LAUNCHER_PID_FILE"

echo "Stopped the local remote worker."
