load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load("@rules_java//java:java_single_jar.bzl", "java_single_jar")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "coverage_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2026.2": [
            "@java_coverage_2026_2//jar",
        ],
        "default": [],
    }),
)

java_library(
    name = "coverage",
    neverlink = 1,
    exports = [":coverage_internal"],
)
