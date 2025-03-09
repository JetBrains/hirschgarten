#!/bin/bash

# create folders to mount in qodana
mkdir plugins
mkdir results
mkdir /tmp/cache

# build plugin
cd ../..
bazel build //plugin-bazel:plugin-bazel_zip

# prepare plugin for qodana
rm -rf tools/qodana/plugins/plugin-bazel
unzip bazel-bin/plugin-bazel/plugin-bazel.zip -d tools/qodana/plugins
