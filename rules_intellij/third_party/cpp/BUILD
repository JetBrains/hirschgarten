#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "cpp_internal",
    visibility = ["//visibility:public"],
    exports = select_for_plugin_api({
        "intellij-2024.3": ["@clion_2024_3//:cpp"],
        "clion-2024.3": ["@clion_2024_3//:cpp"],
        "default": [],
    }),
)

java_library(
    name = "cpp_for_tests",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = [":cpp_internal"],
)

java_library(
    name = "cpp",
    neverlink = 1,
    visibility = ["//visibility:public"],
    exports = [":cpp_internal"],
)
