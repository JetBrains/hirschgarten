/usr/bin/bazel build //plugin-bsp:intellij-bsp_zip
/usr/bin/bazel build //plugin-bazel:intellij-bazel_zip
unzip bazel-bin/plugin-bsp/intellij-bsp.zip -d /opt/idea/custom-plugins
unzip bazel-bin/plugin-bazel/intellij-bazel.zip -d /opt/idea/custom-plugins
