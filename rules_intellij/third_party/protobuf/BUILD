load("@rules_java//java:java_library.bzl", "java_library")

#
# This file is based on Bazel plugin for IntelliJ by The Bazel Authors, licensed under Apache-2.0;
# It was modified by JetBrains s.r.o. and contributors
#
load("//rules_intellij/intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "protoedit_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2025.1": ["@protoedit_2025_1//:protoedit"],
        "intellij-2025.2": ["@protoedit_2025_2//:protoedit"],
        "intellij-2025.3": ["@protoedit_2025_3//:protoedit"],
        "default": [],
    }),
)

java_library(
    name = "protoedit_for_tests",
    testonly = 1,
    exports = [":protoedit_internal"],
)

java_library(
    name = "protoedit",
    neverlink = 1,
    exports = [":protoedit_internal"],
)
