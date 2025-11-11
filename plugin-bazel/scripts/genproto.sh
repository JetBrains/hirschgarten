#!/usr/bin/env sh
. "$(git rev-parse --show-toplevel)/build/protobuf/getprotoc.sh"

script_dir="$(dirname "$0")"
src_dir="$script_dir/../src"
gen_dir="$script_dir/../src/main/gen"

find "$src_dir/main/kotlin" -name "sync.proto" | while read -r proto_file; do
  protoc -I="$src_dir" --java_out="$gen_dir" --java_opt=annotate_code "$proto_file"
done