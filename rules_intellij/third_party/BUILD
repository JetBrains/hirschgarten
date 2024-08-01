#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
package(default_visibility = ["//visibility:public"])

java_library(
    name = "python",
    exports = ["//third_party/python"],
)

java_library(
    name = "truth",
    exports = [
        "@diffutils//jar",
        "@truth//jar",
        "@truth8//jar",
    ],
)
