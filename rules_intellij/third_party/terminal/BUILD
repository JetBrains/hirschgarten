load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "terminal_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2025.3": [
            "@terminal_2025_3//jar",
            "@terminal_completion_2025_3//jar",
        ],
        "default": [],
    }),
)

java_library(
    name = "terminal_for_tests",
    testonly = 1,
    exports = [":terminal_internal"],
)

java_library(
    name = "terminal",
    neverlink = 1,
    exports = [":terminal_internal"],
)
