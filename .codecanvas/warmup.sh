bazel build //... --keep_going --config=remotecache --config=nocacheupload || true
bazel test //... --keep_going --config=remotecache --config=nocacheupload || true
