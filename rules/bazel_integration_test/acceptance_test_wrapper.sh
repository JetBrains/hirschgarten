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
  if has_flag "--escape-aspects-output" "$@"; then
  cat <<'SED'
s|bazel-out/[^"/]*fastbuild/bin|bazel-out/fastbuild/bin|g
s|bazel-out/[^"/]*opt-exec[^"/]*/bin|bazel-out/opt-exec/bin|g
s|external/python[^"/]*|external/python|g
s|external/remotejdk[^"/]*|external/remotejdk|g
s|external/rules_java++toolchains+remotejdk[^"/]*|external/rules_java++toolchains+remotejdk|g
s|external/rules_java~[^"/]*~toolchains~remotejdk[^"/]*|external/rules_java~~toolchains~remotejdk|g
s|external/rules_python++python+python[^"/]*|external/rules_python++python+python|g
s|external/rules_python~[^"/]*~python~python[^"/]*|external/rules_python~~python~python|g
SED
  fi
  if has_flag "--check-diagnostics" "$@"; then
    cat <<'SED'
s|textDocument=TextDocumentIdentifier(path=[^\)]*/\([^)]*\))|textDocument=TextDocumentIdentifier(path=\1)|g
s/\x1b\[[0-9;?]*[ -/]*[@-~]//g
SED
  fi
}

args=()
while (("$#")); do
  case "${1}" in
    "--bazel")
      bazel_binary="${2}"
      shift 2
      ;;
    "--workspace")
      workspace_file_path="${2}"
      shift 2
      ;;
    "--test_runner")
      test_runner="${2}"
      shift 2
      ;;
  "--expected_outputs")
      expected_outputs="${2}"
      shift 2
      ;;
    *)
      args+=("${1}")
      shift 1
      ;;
  esac
done

export BIT_BAZEL_BINARY="${bazel_binary}"
export BIT_WORKSPACE_DIR="${workspace_file_path}"

tmp_raw="${TEST_TMPDIR}/output_raw.txt"
tmp_sed="${TEST_TMPDIR}/output_sed.txt"
touch $tmp_raw
touch $tmp_sed
echo ${args[@]}

"${test_runner}" > $tmp_raw
make_sed_script ${args[@]} | sed -f /dev/stdin -- $tmp_raw > $tmp_sed
diff -u --strip-trailing-cr $expected_outputs $tmp_sed