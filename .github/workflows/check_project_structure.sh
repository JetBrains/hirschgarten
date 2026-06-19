#!/bin/sh

repo_root=$(cd "$(dirname "$0")/../.." && pwd)

find "$repo_root" -type f -name '*.iml' | while IFS= read -r iml_file; do
    dir=$(dirname "$iml_file")

    if [ ! -f "$dir/BUILD" ]; then
        echo "No corresponding BUILD file found for ${iml_file#"$repo_root"/}. Add a ~BUILD file in the monorepo and update dependencies of intellij_plugin_zip_and_debug_target in plugins/bazel/~BUILD" >&2
        exit 1
    fi
done
