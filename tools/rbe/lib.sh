#!/bin/bash

set -euo pipefail

readonly RBE_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPT_REPO_ROOT="$(cd "$RBE_SCRIPT_DIR/../../../.." && pwd)"
readonly BAZEL_LAUNCHER="$SCRIPT_REPO_ROOT/bazel.cmd"

readonly DEFAULT_RBE_PORT="9092"
readonly RBE_USE_LINUX_SANDBOX="${RBE_USE_LINUX_SANDBOX:-0}"

die() {
  echo "$*" >&2
  exit 1
}

resolve_target_workspace_root() {
  local candidate="${1:-$PWD}"

  if [ -f "$candidate" ]; then
    candidate="$(dirname "$candidate")"
  fi

  candidate="$(cd "$candidate" && pwd)"

  while true; do
    if [ -f "$candidate/.bazelversion" ]; then
      echo "$candidate"
      return
    fi

    if [ "$candidate" = "/" ]; then
      break
    fi

    candidate="$(dirname "$candidate")"
  done

  die "Could not find .bazelversion in ${1:-$PWD} or its parent directories."
}

derive_upstream_bazel_version() {
  local raw="$1"

  if [[ "$raw" == */* ]]; then
    raw="${raw#*/}"
  fi

  raw="${raw%%-jb_*}"
  [ -n "$raw" ] || die "Could not derive an upstream Bazel version from .bazelversion value: $1"
  echo "$raw"
}

cache_root() {
  case "$(uname -s)" in
    Darwin)
      echo "$HOME/Library/Caches/JetBrains/bazel-plugin/rbe"
      ;;
    Linux)
      echo "${XDG_CACHE_HOME:-$HOME/.cache}/JetBrains/bazel-plugin/rbe"
      ;;
    *)
      die "Unsupported OS: $(uname -s)"
      ;;
  esac
}

readonly TARGET_WORKSPACE_INPUT="${1:-${RBE_TARGET_WORKSPACE_ROOT:-$PWD}}"
readonly TARGET_WORKSPACE_ROOT="$(resolve_target_workspace_root "$TARGET_WORKSPACE_INPUT")"
readonly TARGET_BAZEL_VERSION_RAW="$(tr -d '[:space:]' < "$TARGET_WORKSPACE_ROOT/.bazelversion")"
readonly UPSTREAM_BAZEL_VERSION="${RBE_UPSTREAM_BAZEL_VERSION:-$(derive_upstream_bazel_version "$TARGET_BAZEL_VERSION_RAW")}"
readonly UPSTREAM_BAZEL_GIT_URL="${RBE_UPSTREAM_BAZEL_GIT_URL:-https://github.com/bazelbuild/bazel.git}"

readonly RBE_CACHE_ROOT="$(cache_root)"
readonly RBE_LAYOUT_ROOT="$RBE_CACHE_ROOT/$UPSTREAM_BAZEL_VERSION"
readonly RBE_CONFIG_FILE="$RBE_LAYOUT_ROOT/config.env"
readonly AUTO_BAZEL_SOURCE_DIR="$RBE_LAYOUT_ROOT/source"

if [ -f "$RBE_CONFIG_FILE" ]; then
  # shellcheck disable=SC1090
  source "$RBE_CONFIG_FILE"
fi

readonly RBE_PORT="${RBE_PORT:-${SAVED_RBE_PORT:-$DEFAULT_RBE_PORT}}"

if [ -n "${RBE_BAZEL_SOURCE_DIR:-${SAVED_BAZEL_SOURCE_DIR:-}}" ]; then
  readonly BAZEL_SOURCE_DIR="${RBE_BAZEL_SOURCE_DIR:-$SAVED_BAZEL_SOURCE_DIR}"
else
  readonly BAZEL_SOURCE_DIR="$AUTO_BAZEL_SOURCE_DIR"
fi

readonly RBE_SOURCE_METADATA_FILE="$AUTO_BAZEL_SOURCE_DIR/.jb-rbe-source.env"

readonly RBE_STATE_ROOT="$RBE_LAYOUT_ROOT/state"
readonly RBE_WORK_PATH="$RBE_STATE_ROOT/work"
readonly RBE_CAS_PATH="$RBE_STATE_ROOT/cas"
readonly RBE_LOG_PATH="$RBE_STATE_ROOT/remote-worker.log"
readonly RBE_PID_FILE="$RBE_STATE_ROOT/remote-worker.pid"
readonly RBE_LAUNCHER_PID_FILE="$RBE_STATE_ROOT/remote-worker.launcher.pid"

readonly WORKSPACE_USER_BAZELRC="$TARGET_WORKSPACE_ROOT/.bazelrc-user.bazelrc"

if [ -d "$TARGET_WORKSPACE_ROOT/plugins/bazel" ]; then
  readonly TARGET_PLUGIN_ROOT="$TARGET_WORKSPACE_ROOT/plugins/bazel"
  readonly PLUGIN_USER_BAZELRC="$TARGET_PLUGIN_ROOT/.user.bazelrc"
else
  readonly TARGET_PLUGIN_ROOT=""
  readonly PLUGIN_USER_BAZELRC=""
fi

ensure_prerequisites() {
  command -v git >/dev/null 2>&1 || die "git is required"
  command -v bash >/dev/null 2>&1 || die "bash is required"
  [ -x "$BAZEL_LAUNCHER" ] || die "Missing Bazel launcher: $BAZEL_LAUNCHER"
}

ensure_state_directories() {
  mkdir -p "$RBE_STATE_ROOT" "$RBE_WORK_PATH" "$RBE_CAS_PATH"
}

is_bazel_workspace_dir() {
  local dir="$1"
  [ -e "$dir/MODULE.bazel" ] || [ -e "$dir/WORKSPACE" ] || [ -e "$dir/WORKSPACE.bazel" ]
}

is_auto_managed_bazel_source_dir() {
  [ "$BAZEL_SOURCE_DIR" = "$AUTO_BAZEL_SOURCE_DIR" ]
}

auto_source_is_current() {
  [ -f "$RBE_SOURCE_METADATA_FILE" ] || return 1
  # shellcheck disable=SC1090
  source "$RBE_SOURCE_METADATA_FILE"
  [ "${SOURCE_READY:-0}" = "1" ] || return 1
  [ "${SOURCE_UPSTREAM_TAG:-}" = "$UPSTREAM_BAZEL_VERSION" ] || return 1
  [ "${SOURCE_UPSTREAM_REPO_URL:-}" = "$UPSTREAM_BAZEL_GIT_URL" ] || return 1
}

record_auto_source_metadata() {
  cat > "$RBE_SOURCE_METADATA_FILE" <<EOF
SOURCE_READY=1
SOURCE_UPSTREAM_TAG='$UPSTREAM_BAZEL_VERSION'
SOURCE_UPSTREAM_REPO_URL='$UPSTREAM_BAZEL_GIT_URL'
EOF
}

prepare_auto_managed_bazel_source_checkout() {
  if [ -e "$AUTO_BAZEL_SOURCE_DIR/.git" ] && is_bazel_workspace_dir "$AUTO_BAZEL_SOURCE_DIR" && auto_source_is_current; then
    return
  fi

  rm -rf "$AUTO_BAZEL_SOURCE_DIR"
  mkdir -p "$(dirname "$AUTO_BAZEL_SOURCE_DIR")"
  git clone --depth 1 --branch "$UPSTREAM_BAZEL_VERSION" --single-branch "$UPSTREAM_BAZEL_GIT_URL" "$AUTO_BAZEL_SOURCE_DIR"

  is_bazel_workspace_dir "$AUTO_BAZEL_SOURCE_DIR" || die "Cloned upstream Bazel checkout is not a Bazel workspace: $AUTO_BAZEL_SOURCE_DIR"
  record_auto_source_metadata
}

ensure_bazel_source_checkout() {
  if is_auto_managed_bazel_source_dir; then
    prepare_auto_managed_bazel_source_checkout
    return
  fi

  if [ -e "$BAZEL_SOURCE_DIR/.git" ] && is_bazel_workspace_dir "$BAZEL_SOURCE_DIR"; then
    return
  fi

  die "Configured Bazel source checkout does not look like a Bazel workspace: $BAZEL_SOURCE_DIR
Set RBE_BAZEL_SOURCE_DIR to a full Bazel source checkout and rerun the script."
}

build_worker_binary() {
  (
    cd "$BAZEL_SOURCE_DIR"
    "$BAZEL_LAUNCHER" build //src/tools/remote:worker
  )
}

worker_binary_path() {
  local bazel_bin_dir
  bazel_bin_dir="$(
    cd "$BAZEL_SOURCE_DIR"
    "$BAZEL_LAUNCHER" info bazel-bin | tail -n 1
  )"
  echo "$bazel_bin_dir/src/tools/remote/worker"
}

worker_pid() {
  if [ -s "$RBE_PID_FILE" ]; then
    cat "$RBE_PID_FILE"
  fi
}

launcher_pid() {
  if [ -s "$RBE_LAUNCHER_PID_FILE" ]; then
    cat "$RBE_LAUNCHER_PID_FILE"
  fi
}

is_pid_running() {
  local pid="$1"
  [ -n "$pid" ] || return 1
  kill -0 "$pid" >/dev/null 2>&1
}

worker_is_running() {
  local pid
  pid="$(worker_pid || true)"
  is_pid_running "$pid"
}

orphaned_worker_pid() {
  local pid
  for pid in $(listening_pids_for_port); do
    [ -n "$pid" ] || continue
    if [ -n "$(worker_pid || true)" ] && [ "$pid" = "$(worker_pid || true)" ]; then
      continue
    fi
    ps -p "$pid" -o command= 2>/dev/null | grep -F "com.google.devtools.build.remote.worker.RemoteWorker" >/dev/null || continue
    ps -p "$pid" -o command= 2>/dev/null | grep -F -- "--work_path=$RBE_WORK_PATH" >/dev/null || continue
    echo "$pid"
    return
  done
}

effective_worker_pid() {
  local pid
  pid="$(worker_pid || true)"
  if is_pid_running "$pid"; then
    echo "$pid"
    return
  fi

  orphaned_worker_pid || true
}

cleanup_stale_pid_files() {
  local pid
  pid="$(worker_pid || true)"
  if [ -n "$pid" ] && ! is_pid_running "$pid"; then
    rm -f "$RBE_PID_FILE"
  fi

  pid="$(launcher_pid || true)"
  if [ -n "$pid" ] && ! is_pid_running "$pid"; then
    rm -f "$RBE_LAUNCHER_PID_FILE"
  fi
}

listening_pids_for_port() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -tiTCP:"$RBE_PORT" -sTCP:LISTEN 2>/dev/null || true
  fi
}

ensure_port_available() {
  local listening_pids current_worker_pid
  current_worker_pid="$(effective_worker_pid || true)"
  listening_pids="$(listening_pids_for_port | tr '\n' ' ')"
  if [ -z "$listening_pids" ]; then
    return
  fi

  if [ -n "$current_worker_pid" ] && [[ " $listening_pids " == *" $current_worker_pid "* ]]; then
    return
  fi

  die "Port $RBE_PORT is already in use by another process. Set RBE_PORT to a different value and rerun the script."
}

linux_sandbox_args() {
  if [ "$RBE_USE_LINUX_SANDBOX" = "0" ]; then
    return
  fi

  case "$(uname -s)" in
    Linux)
      echo "--sandboxing --sandboxing_tmpfs_dir=/tmp"
      ;;
    *)
      die "RBE_USE_LINUX_SANDBOX=1 is only supported on Linux"
      ;;
  esac
}

wait_for_worker() {
  local attempts=0
  while [ "$attempts" -lt 120 ]; do
    if worker_is_running; then
      return
    fi
    sleep 1
    attempts=$((attempts + 1))
  done

  echo "Timed out waiting for the local remote worker to start." >&2
  print_log_tail >&2
  exit 1
}

print_log_tail() {
  if [ -f "$RBE_LOG_PATH" ]; then
    tail -n 40 "$RBE_LOG_PATH"
  else
    echo "No worker log found at $RBE_LOG_PATH"
  fi
}

render_rbe_profile() {
  cat <<EOF
common:rbe-local --remote_cache=grpc://127.0.0.1:$RBE_PORT
common:rbe-local --remote_upload_local_results=true
common:rbe-local --remote_cache_compression=false
common:rbe-local --spawn_strategy=remote,local
common:rbe-local --genrule_strategy=remote,local
common:rbe-local --strategy=JvmCompile=worker
common:rbe-local --remote_local_fallback
common:rbe-local --remote_executor=grpc://127.0.0.1:$RBE_PORT
EOF
}

render_user_bazelrc() {
  cat <<EOF
# Generated by plugins/bazel/tools/rbe/setup.sh.
# Delete this file to disable the local single-node remote execution worker.
build --config=rbe-local
test --config=rbe-local
run --config=rbe-local
coverage --config=rbe-local
$(render_rbe_profile)
EOF
}

write_runtime_config() {
  mkdir -p "$RBE_LAYOUT_ROOT"
  cat > "$RBE_CONFIG_FILE" <<EOF
SAVED_RBE_PORT='$RBE_PORT'
SAVED_BAZEL_SOURCE_DIR='$BAZEL_SOURCE_DIR'
SAVED_TARGET_WORKSPACE_ROOT='$TARGET_WORKSPACE_ROOT'
SAVED_TARGET_BAZEL_VERSION_RAW='$TARGET_BAZEL_VERSION_RAW'
EOF
}

write_user_bazelrcs() {
  render_user_bazelrc > "$WORKSPACE_USER_BAZELRC"

  if [ -n "$PLUGIN_USER_BAZELRC" ]; then
    render_user_bazelrc > "$PLUGIN_USER_BAZELRC"
  fi
}

workspace_imports_root_user_bazelrc() {
  [ -f "$TARGET_WORKSPACE_ROOT/.bazelrc" ] || return 1
  grep -Fq 'try-import %workspace%/.bazelrc-user.bazelrc' "$TARGET_WORKSPACE_ROOT/.bazelrc"
}

plugin_imports_user_bazelrc() {
  [ -n "$TARGET_PLUGIN_ROOT" ] || return 1
  [ -f "$TARGET_PLUGIN_ROOT/.bazelrc" ] || return 1
  grep -Fq 'try-import %workspace%/.user.bazelrc' "$TARGET_PLUGIN_ROOT/.bazelrc"
}

print_bazelrc_hook_notes() {
  if ! workspace_imports_root_user_bazelrc; then
    echo "  Note: $TARGET_WORKSPACE_ROOT/.bazelrc does not import .bazelrc-user.bazelrc."
    echo "        Add: try-import %workspace%/.bazelrc-user.bazelrc"
  fi

  if [ -n "$PLUGIN_USER_BAZELRC" ] && ! plugin_imports_user_bazelrc; then
    echo "  Note: $TARGET_PLUGIN_ROOT/.bazelrc does not import .user.bazelrc."
    echo "        Add: try-import %workspace%/.user.bazelrc"
  fi
}

print_status_summary() {
  local effective_pid
  effective_pid="$(effective_worker_pid || true)"

  echo "Local remote execution worker"
  echo "  Target root:    $TARGET_WORKSPACE_ROOT"
  echo "  Bazel version:  $TARGET_BAZEL_VERSION_RAW"
  echo "  Upstream Bazel: $UPSTREAM_BAZEL_VERSION"
  echo "  Source repo:    $UPSTREAM_BAZEL_GIT_URL"
  echo "  Source dir:     $BAZEL_SOURCE_DIR"
  echo "  Port:           $RBE_PORT"
  echo "  Remote cache:   grpc://127.0.0.1:$RBE_PORT"
  echo "  Worker log:     $RBE_LOG_PATH"
  echo "  Root bazelrc:   $WORKSPACE_USER_BAZELRC"
  if [ -n "$PLUGIN_USER_BAZELRC" ]; then
    echo "  Plugin bazelrc: $PLUGIN_USER_BAZELRC"
  fi

  if [ -n "$effective_pid" ]; then
    if [ -n "$(worker_pid || true)" ] && [ "$effective_pid" = "$(worker_pid || true)" ]; then
      echo "  Status:         running (pid $effective_pid)"
    else
      echo "  Status:         running (pid $effective_pid, recovered from port)"
    fi
  else
    echo "  Status:         stopped"
  fi
}
