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
s|      root_execution_path_fragment: "bazel-out/[^"]*fastbuild/bin"|      root_execution_path_fragment: "bazel-out/fastbuild/bin"|g
s|    root_execution_path_fragment: "bazel-out/[^"]*fastbuild/bin"|    root_execution_path_fragment: "bazel-out/fastbuild/bin"|g
s|    root_execution_path_fragment: "bazel-out/[^"]*opt-exec[^"]*/bin"|    root_execution_path_fragment: "bazel-out/opt-exec/bin"|g
s|    root_execution_path_fragment: "external/python[^"]*"|    root_execution_path_fragment: "external/python"|g
s|    root_execution_path_fragment: "external/remotejdk[^"]*"|    root_execution_path_fragment: "external/remotejdk"|g
s|    root_execution_path_fragment: "external/rules_java++toolchains+remotejdk[^"]*"|    root_execution_path_fragment: "external/rules_java++toolchains+remotejdk"|g
s|    root_execution_path_fragment: "external/rules_java~[^"]*~toolchains~remotejdk[^"]*"|    root_execution_path_fragment: "external/rules_java~~toolchains~remotejdk"|g
s|    root_execution_path_fragment: "external/rules_python++python+python[^"]*"|    root_execution_path_fragment: "external/rules_python++python+python"|g
s|    root_execution_path_fragment: "external/rules_python~[^"]*~python~python[^"]*"|    root_execution_path_fragment: "external/rules_python~~python~python"|g
s|  jvm_flags: "--some_flag=bazel-out/[^"]*fastbuild/bin/java_targets/libjava_library.jar"|  jvm_flags: "--some_flag=bazel-out/fastbuild/bin/java_targets/libjava_library.jar"|g
s|  value: "bazel-out/[^"]*fastbuild/bin/environment_variables/java_binary bazel-out/[^"]*fastbuild/bin/environment_variables/java_binary.jar"|  value: "bazel-out/fastbuild/bin/environment_variables/java_binary bazel-out/fastbuild/bin/environment_variables/java_binary.jar"|g
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