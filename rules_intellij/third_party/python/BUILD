load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "python_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2024.3": ["@python_2024_3//:python"],
        "intellij-2025.1": ["@python_2025_1//:python"],
        "intellij-2025.2": ["@python_2025_2//:python"],
    }),
)

filegroup(
    name = "python_helpers",
    srcs = select_for_plugin_api({
        "intellij-2024.3": ["@python_2024_3//:python_helpers"],
        "intellij-2025.1": ["@python_2025_1//:python_helpers"],
        "intellij-2025.2": ["@python_2025_2//:python_helpers"],
    }),
    visibility = ["//visibility:private"],
)

java_library(
    name = "python_for_tests",
    testonly = 1,
    data = [":python_helpers"],
    exports = [
        ":python_internal",
        # Python plugin has several non-optional plugin dependencies
        "//rules_intellij/third_party/json:json_for_tests",
        "//rules_intellij/third_party/toml:toml_for_tests",
    ],
)

java_library(
    name = "python",
    neverlink = 1,
    exports = [":python_internal"],
)
