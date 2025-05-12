load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "cpp_internal",
    visibility = ["//visibility:public"],
    exports = select_for_plugin_api({
        "intellij-2024.3": ["@clion_2024_3//:cpp"],
        "intellij-2025.1": ["@clion_2025_1//:cpp"],
        "intellij-2025.2": ["@clion_2025_2//:cpp"],
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
