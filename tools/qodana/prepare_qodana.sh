#!/bin/bash

# create folders to mount in qodana
mkdir plugins
mkdir results
mkdir /tmp/cache

# build plugins
cd ../..
bazel build //plugin-bsp:intellij-bsp_zip
bazel build //plugin-bazel:intellij-bazel_zip

# prepare plugins for qodana
rm -rf tools/qodana/plugins/intellij-bsp
unzip bazel-bin/plugin-bsp/intellij-bsp.zip -d tools/qodana/plugins
rm -rf tools/qodana/plugins/intellij-bazel
unzip bazel-bin/plugin-bazel/intellij-bazel.zip -d tools/qodana/plugins
