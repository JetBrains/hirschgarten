#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

java_library(
    name = "toml_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2024.1": ["@toml_2024_1//:toml"],
        "intellij-2024.2": ["@toml_2024_2//:toml"],
        "default": [],
    }),
)

java_library(
    name = "toml_for_tests",
    testonly = 1,
    visibility = ["//python:__pkg__"],
    exports = [":toml_internal"],
)
