load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

java_library(
    name = "toml_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2025.1": [
            "@toml_2025_1//jar",
            "@toml_core_2025_1//jar",
            "@toml_json_2025_1//jar",
            "@toml_grazie_2025_1//jar",
        ],
        "intellij-2025.2": [
            "@toml_plugin_2025_2//:toml",
        ],
        "intellij-2025.3": [
            "@toml_plugin_2025_3//:toml",
        ],
        "default": [],
    }),
)

java_library(
    name = "toml",
    neverlink = 1,
    exports = ["toml_internal"],
)

java_library(
    name = "toml_for_tests",
    testonly = 1,
    exports = [":toml_internal"],
)
