#!/bin/sh

repo_root=$(cd "$(dirname "$0")/../.." && pwd)

is_bazelignored() {
    rel_path=$1
    [ -f "$repo_root/.bazelignore" ] || return 1
    while IFS= read -r ignored_dir || [ -n "$ignored_dir" ]; do
        case "$ignored_dir" in
            ''|'#'*) continue ;;
        esac
        case "$rel_path" in
            "$ignored_dir"/*) return 0 ;;
        esac
    done < "$repo_root/.bazelignore"
    return 1
}

find "$repo_root" -type f -name '*.iml' | while IFS= read -r iml_file; do
    rel_iml=${iml_file#"$repo_root"/}

    if is_bazelignored "$rel_iml"; then
        continue
    fi

    dir=$(dirname "$iml_file")

    if [ ! -f "$dir/BUILD" ] && [ ! -f "$dir/BUILD.bazel" ]; then
        echo "No BUILD or BUILD.bazel file found for $rel_iml. Regenerate BUILD.bazel in the monorepo (./build/jpsModelToBazel.cmd) or add a ~BUILD override, then update the srcs of intellij_plugin_zip_and_debug_target in plugins/bazel/~BUILD" >&2
        exit 1
    fi
done
